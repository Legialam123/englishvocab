package com.englishvocab.controller;

import com.englishvocab.dto.SessionResultDTO;
import com.englishvocab.dto.SessionResultRequest;
import com.englishvocab.dto.SessionVocabularyDTO;
import com.englishvocab.dto.VocabWithProgressDTO;
import com.englishvocab.entity.Dictionary;
import com.englishvocab.entity.LearningSession;
import com.englishvocab.entity.SessionVocabulary;
import com.englishvocab.entity.Topics;
import com.englishvocab.entity.User;
import com.englishvocab.entity.Vocab;
import com.englishvocab.entity.UserVocabProgress;
import com.englishvocab.security.CustomUserPrincipal;
import com.englishvocab.repository.UserRepository;
import com.englishvocab.service.DictionaryService;
import com.englishvocab.service.LearningService;
import com.englishvocab.service.TopicsService;
import com.englishvocab.service.UserProgressService;
import com.englishvocab.service.VocabularyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller xử lý các chức năng học từ vựng với Redis caching
 * 
 * Learning Modes:
 * 1. Review - Ôn tập từ cần review (ưu tiên)
 * 2. New - Học từ mới
 * 3. Alphabetical - Học theo thứ tự A-Z
 * 4. Topics - Học theo chủ đề 
 * 5. Custom - Tự chọn từ vựng
 * 
 * @author EnglishVocab Team
 */
@Controller
@RequestMapping("/learn")
@RequiredArgsConstructor
@Slf4j
public class LearningController {

    private final DictionaryService dictionaryService;
    private final UserProgressService userProgressService;
    private final TopicsService topicsService;
    private final VocabularyService vocabularyService;
    private final LearningService learningService;
    private final UserRepository userRepository;

    /**
     * 📝 ALPHABETICAL LEARNING MODE
     * Học từ vựng theo thứ tự A-Z
     */
    @GetMapping("/dictionary/{dictionaryId}/alphabetical")
    public String alphabeticalMode(
        @PathVariable Integer dictionaryId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String startLetter,
        @RequestParam(required = false) String letters,
        @RequestParam(required = false) String level,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            // Validate dictionary exists
            Dictionary dictionary = dictionaryService.findByIdOrThrow(dictionaryId);
            model.addAttribute("dictionary", dictionary);

            // Get current user
            String currentUserId = getCurrentUserId(authentication);
            model.addAttribute("currentUserId", currentUserId);
            User user = userRepository.findByEmail(currentUserId)
            .orElseThrow(() -> new RuntimeException("User không tồn tại"));

            Pageable pageable = PageRequest.of(page - 1, size);
            Page<Vocab> vocabularies;

            List<String> activeLetterWindow = null;
            String lettersDisplay = null;
            List<String> requestedLetters = null;

            // Process letters filter
            if (letters != null && !letters.isBlank()) {
                String sanitizedLetters = letters.replaceAll("[^a-zA-Z]", "");
                if (!sanitizedLetters.isEmpty()) {
                    lettersDisplay = sanitizedLetters.toUpperCase(Locale.ROOT)
                            .chars()
                            .mapToObj(character -> String.valueOf((char) character))
                            .collect(Collectors.joining(", "));
                }
                requestedLetters = sanitizedLetters.chars()
                        .mapToObj(character -> String.valueOf((char) character).toLowerCase(Locale.ROOT))
                        .collect(Collectors.toList());

                if (!requestedLetters.isEmpty()) {
                    activeLetterWindow = requestedLetters.stream()
                            .map(letter -> letter.toUpperCase(Locale.ROOT))
                            .collect(Collectors.toList());
                }
            }

            // Process single startLetter filter
            if (activeLetterWindow == null && startLetter != null && !startLetter.isEmpty()) {
                requestedLetters = List.of(startLetter.toLowerCase());
                activeLetterWindow = List.of(startLetter.substring(0, 1).toUpperCase(Locale.ROOT));
            }

            // Apply combined filters
            if (requestedLetters != null && !requestedLetters.isEmpty() && level != null && !level.isEmpty()) {
                // Both letter and level filters
                vocabularies = vocabularyService.findByDictionaryAndWordStartingWithAndLevel(dictionaryId, requestedLetters, level, pageable);
            } else if (requestedLetters != null && !requestedLetters.isEmpty()) {
                // Only letter filter
                vocabularies = vocabularyService.findByDictionaryAndWordStartingWith(dictionaryId, requestedLetters, pageable);
            } else if (level != null && !level.isEmpty()) {
                // Only level filter
                vocabularies = vocabularyService.findByDictionaryAndLevel(dictionaryId, level, pageable);
            } else {
                // No filters
                vocabularies = vocabularyService.findByDictionaryOrderByWordAsc(dictionaryId, pageable);
            }

            // Calculate total based on applied filters
            long total;
            if (requestedLetters != null && !requestedLetters.isEmpty() && level != null && !level.isEmpty()) {
                // Count with both filters
                total = vocabularyService.countByDictionaryAndWordStartingWithAndLevel(dictionaryId, requestedLetters, level);
            } else if (requestedLetters != null && !requestedLetters.isEmpty()) {
                // Count with letter filter only
                total = vocabularyService.countByDictionaryAndWordStartingWith(dictionaryId, requestedLetters);
            } else if (level != null && !level.isEmpty()) {
                // Count with level filter only
                total = vocabularyService.countByDictionaryAndLevel(dictionaryId, level);
            } else {
                // Count all
                total = vocabularyService.countByDictionary(dictionary.getDictionaryId());
            }

            long learned = userProgressService.countLearnedWordsInDictionary(user, dictionary);
            long review = userProgressService.countWordsForReviewInDictionary(user, dictionary);
            long inProgress = Math.max(total - learned - review, 0);
            double percent = total == 0 ? 0 : (learned * 100.0 / total);

            // Tạo DTO với progress information cho mỗi vocab
            List<VocabWithProgressDTO> vocabsWithProgress = vocabularies.getContent().stream()
                .map(vocab -> {
                    UserVocabProgress progress = userProgressService.findUserProgress(user, vocab);
                    return VocabWithProgressDTO.of(vocab, progress);
                })
                .collect(Collectors.toList());

            model.addAttribute("inProgressCount", inProgress);
            model.addAttribute("progressPercent", percent);
            model.addAttribute("vocabularies", vocabsWithProgress);
            model.addAttribute("totalVocab", total);
            model.addAttribute("learnedVocab", learned);
            model.addAttribute("reviewVocab", review);
            model.addAttribute("totalPages", vocabularies.getTotalPages());
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("startLetter", startLetter);
            model.addAttribute("letters", letters);
            model.addAttribute("activeLetterWindow", activeLetterWindow != null ? activeLetterWindow : List.of());
            model.addAttribute("lettersDisplay", lettersDisplay);
            Map<String, Long> letterCounts = vocabularyService.getVocabCountByFirstLetter(dictionaryId);
            model.addAttribute("letterCounts", letterCounts);
            model.addAttribute("level", level);
            model.addAttribute("pageTitle", "Học từ vựng: " + dictionary.getName() + " (A-Z)");
            model.addAttribute("learningMode", "alphabetical");
            
        log.info("User {} started alphabetical learning for dictionary {}",
            currentUserId, dictionary.getName());

            return "learn/alphabetical";

        } catch (RuntimeException e) {
            log.error("Error loading alphabetical learning mode", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/vocabulary/dictionaries";
        }
    }

    /**
     * 🏷️ TOPICS LEARNING MODE  
     * Học từ vựng theo chủ đề
     */
    @GetMapping("/dictionary/{dictionaryId}/topics")
    public String topicsMode(
            @PathVariable Integer dictionaryId,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            // Validate dictionary exists
            Dictionary dictionary = dictionaryService.findByIdOrThrow(dictionaryId);
            model.addAttribute("dictionary", dictionary);

            // Get current user
            String currentUserId = getCurrentUserId(authentication);
            model.addAttribute("currentUserId", currentUserId);

            // Load topics with vocabulary counts for this dictionary
            List<Topics> topics = topicsService.findActiveTopics();
            model.addAttribute("topics", topics);
            
            // Load vocabulary counts per topic for this dictionary
            Map<Integer, Long> topicVocabCounts = vocabularyService.getVocabCountByTopicsForDictionary(dictionaryId);
            model.addAttribute("topicVocabCounts", topicVocabCounts);

            model.addAttribute("pageTitle", "Học từ vựng: " + dictionary.getName() + " (Chủ đề)");
            model.addAttribute("learningMode", "topics");

            log.info("User {} started topics learning for dictionary {}", 
                    currentUserId, dictionary.getName());

            return "learn/topics";

        } catch (RuntimeException e) {
            log.error("Error loading topics learning mode", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/vocabulary/dictionaries";
        }
    }

    /**
     * ⚡ CUSTOM LEARNING MODE
     * Tự chọn từ vựng để học
     */
    @GetMapping("/dictionary/{dictionaryId}/custom")
    public String customMode(
            @PathVariable Integer dictionaryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) List<Integer> topicIds,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            // Validate dictionary exists
            Dictionary dictionary = dictionaryService.findByIdOrThrow(dictionaryId);
            model.addAttribute("dictionary", dictionary);

            // Get current user
            String currentUserId = getCurrentUserId(authentication);
            model.addAttribute("currentUserId", currentUserId);
            User user = userRepository.findByEmail(currentUserId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

            // Load vocabulary with advanced filtering from database
            Pageable pageable = PageRequest.of(page - 1, size);
            Page<Vocab> vocabularies = vocabularyService.findByDictionaryWithFilters(
                dictionaryId, search, level, topicIds, pageable);

            // Load available topics for filtering
            List<Topics> availableTopics = topicsService.findActiveTopics();
            model.addAttribute("availableTopics", availableTopics);
            
            // Tạo DTO với progress information cho mỗi vocab
            List<VocabWithProgressDTO> vocabsWithProgress = vocabularies.getContent().stream()
                .map(vocab -> {
                    UserVocabProgress progress = userProgressService.findUserProgress(user, vocab);
                    return VocabWithProgressDTO.of(vocab, progress);
                })
                .collect(Collectors.toList());
            
            // Add vocabulary data to model
            model.addAttribute("vocabularies", vocabsWithProgress);
            model.addAttribute("totalPages", vocabularies.getTotalPages());
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("search", search);
            model.addAttribute("level", level);
            model.addAttribute("selectedTopicIds", topicIds);

            model.addAttribute("pageTitle", "Học từ vựng: " + dictionary.getName() + " (Tự chọn)");
            model.addAttribute("learningMode", "custom");

            log.info("User {} started custom learning for dictionary {}", 
                    currentUserId, dictionary.getName());

            return "learn/custom";

        } catch (RuntimeException e) {
            log.error("Error loading custom learning mode", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/vocabulary/dictionaries";
        }
    }

    /**
     * 🎯 START CUSTOM VOCAB LEARNING SESSION
     * Bắt đầu session học với danh sách từ vựng được chọn từ alphabetical mode
     * Endpoint này nhận list các vocabId và tạo session học chỉ với những từ đã chọn
     */
    @PostMapping("/dictionary/{dictionaryId}/custom-session")
    @ResponseBody
    public ResponseEntity<?> startCustomVocabSession(
            @PathVariable Integer dictionaryId,
            @RequestBody List<Long> vocabIds,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            // Validate input
            if (vocabIds == null || vocabIds.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Vui lòng chọn ít nhất 1 từ vựng"));
            }

            // Get current user
            String userEmail = getCurrentUserId(authentication);
            log.info("User {} starting custom vocab session with {} selected words", 
                    userEmail, vocabIds.size());

            User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
            
            // Get dictionary
            Dictionary dictionary = dictionaryService.findByIdOrThrow(dictionaryId);

            // Convert Long to Integer for vocabIds
            List<Integer> vocabIdInts = vocabIds.stream()
                .map(Long::intValue)
                .collect(Collectors.toList());

            // Create learning session with selected vocab IDs
            LearningSession session = learningService.createSession(
                user, dictionary, "custom", vocabIdInts, vocabIds.size());

            log.info("User {} created custom vocab session {} with {} words for dictionary {}", 
                    userEmail, session.getSessionUuid(), vocabIds.size(), dictionary.getName());

            // Return redirect URL
            String redirectUrl = "/learn/session/flashcards?sessionId=" + session.getSessionUuid();
            return ResponseEntity.ok(Map.of("redirectUrl", redirectUrl));

        } catch (RuntimeException e) {
            log.error("Error starting custom vocab session for user {} dictionary {} vocabIds {}", 
                    getSafeUserEmail(authentication), dictionaryId, vocabIds, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Có lỗi xảy ra: " + e.getMessage()));
        }
    }

    /**
     * 🎯 START LEARNING SESSION
     * Bắt đầu session học với từ vựng đã chọn
     * Session được cache trong Redis với TTL 30 phút
     */
    @PostMapping("/session/start")
    public String startLearningSession(
            @RequestParam Integer dictionaryId,
            @RequestParam String learningMode,
            @RequestParam(required = false) List<Integer> selectedVocabIds,
            @RequestParam(required = false) List<Integer> topicIds,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String startLetter,
            @RequestParam(defaultValue = "20") int sessionSize,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            // Get current user
            String userEmail = getCurrentUserId(authentication);
            if (log.isDebugEnabled()) {
                log.debug("Start session request from user={} dictionaryId={} mode={} sessionSize={} level={} startLetter={} selectedVocabIds={} topicIds={}",
                        userEmail, dictionaryId, learningMode, sessionSize, level, startLetter, selectedVocabIds, topicIds);
            }

            User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
            
            // Get dictionary
            Dictionary dictionary = dictionaryService.findByIdOrThrow(dictionaryId);

            // Create learning session (cached in Redis) with level and startLetter filters
            LearningSession session = learningService.createSession(
                user, dictionary, learningMode, selectedVocabIds, sessionSize, level, startLetter);

            log.info("User {} started learning session {} for dictionary {} in {} mode with level={} startLetter={}", 
                    userEmail, session.getSessionUuid(), dictionary.getName(), learningMode, level, startLetter);

            // Redirect to flashcard session
            return "redirect:/learn/session/flashcards?sessionId=" + session.getSessionUuid();

        } catch (RuntimeException e) {
            log.error("Error starting learning session for user {} dictionary {} mode {} selectedVocabIds {} topicIds {}", 
                    getSafeUserEmail(authentication), dictionaryId, learningMode, selectedVocabIds, topicIds, e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/vocabulary/dictionary/" + dictionaryId;
        }
    }

    /**
     * 🃏 FLASHCARD SESSION
     * Interface học với flashcards (cached in Redis)
     */
    @GetMapping("/session/flashcards")
    public String flashcardSession(
            @RequestParam String sessionId,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            String userEmail = getCurrentUserId(authentication);

            // Load learning session from cache/database
            LearningSession session = learningService.getSessionByUuid(sessionId);
            
            // Verify user owns this session
            if (!session.getUser().getEmail().equals(userEmail)) {
                throw new RuntimeException("Không có quyền truy cập session này");
            }
            
            // Get session vocabularies with pagination
            Page<SessionVocabulary> vocabularies = learningService.getSessionVocabularies(
                sessionId, PageRequest.of(0, 100));
            
            // Convert to DTOs to avoid LazyInitializationException
            List<SessionVocabularyDTO> vocabularyDTOs = vocabularies.getContent().stream()
                .map(SessionVocabularyDTO::from)
                .collect(Collectors.toList());
            
            model.addAttribute("session", session);
            model.addAttribute("vocabularies", vocabularyDTOs);
            model.addAttribute("dictionary", session.getDictionary());
            model.addAttribute("pageTitle", "Học từ vựng - Flashcards");

            log.info("User {} accessing flashcard session {} with {} vocabularies", 
                userEmail, sessionId, vocabularyDTOs.size());

            return "learn/flashcards";

        } catch (RuntimeException e) {
            log.error("Error loading flashcard session", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/vocabulary/dictionaries";
        }
    }
    
    /**
     * 🎯 RECORD ANSWER API
     * Record user answer (update Redis cache)
     */
    @PostMapping("/session/answer")
    @ResponseBody
    public ResponseEntity<?> recordAnswer(
            @RequestParam String sessionId,
            @RequestParam Integer vocabId,
            @RequestParam String answerType, // CORRECT, WRONG, SKIP
            @RequestParam(defaultValue = "0") Integer timeSpent,
            Authentication authentication) {

        try {
            String userEmail = getCurrentUserId(authentication);
            
            // Record answer (updates cache automatically)
            SessionVocabulary.AnswerType answer = SessionVocabulary.AnswerType.valueOf(answerType);
            LearningSession session = learningService.recordAnswer(sessionId, vocabId, answer, timeSpent);

            log.debug("User {} recorded {} for vocab {} in session {}", 
                userEmail, answerType, vocabId, sessionId);

            return ResponseEntity.ok().body(Map.of(
                "success", true,
                "correctCount", session.getCorrectCount(),
                "wrongCount", session.getWrongCount(),
                "skipCount", session.getSkipCount(),
                "progress", session.getProgress()
            ));

        } catch (RuntimeException e) {
            log.error("Error recording answer", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * ⏸️ PAUSE SESSION API
     */
    @PostMapping("/session/pause")
    @ResponseBody
    public ResponseEntity<?> pauseSession(
            @RequestParam String sessionId,
            Authentication authentication) {

        try {
            learningService.pauseSession(sessionId);
            return ResponseEntity.ok().body(Map.of("success", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * ▶️ RESUME SESSION API
     */
    @PostMapping("/session/resume")
    @ResponseBody
    public ResponseEntity<?> resumeSession(
            @RequestParam String sessionId,
            Authentication authentication) {

        try {
            learningService.resumeSession(sessionId);
            return ResponseEntity.ok().body(Map.of("success", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 📊 SESSION COMPLETE
     * Hoàn thành session và hiển thị kết quả (evict from cache)
     */
    @PostMapping("/session/complete")
    @ResponseBody
    public ResponseEntity<?> completeSession(
            @RequestBody SessionResultRequest request,
            Authentication authentication) {

        try {
            getCurrentUserId(authentication);

            // Complete session and update progress (batch update)
            SessionResultDTO result = learningService.completeSession(
                request.getSessionId(), request);

            // Return success response for AJAX
            return ResponseEntity.ok().body(Map.of(
                "success", true,
                "message", "Session completed successfully",
                "redirectUrl", "/learn/session/results?sessionId=" + request.getSessionId(),
                "result", result
            ));

        } catch (RuntimeException e) {
            log.error("Error completing learning session", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 🏆 SESSION RESULTS
     * Hiển thị kết quả sau khi hoàn thành session
     */
    @GetMapping("/session/results")
    public String sessionResults(
            @RequestParam String sessionId,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            String userEmail = getCurrentUserId(authentication);

            // Load session (from database, no longer in cache)
            LearningSession session = learningService.getSessionByUuid(sessionId);
            SessionResultDTO result = learningService.getSessionResult(sessionId);
            
            // Verify user owns this session
            if (!session.getUser().getEmail().equals(userEmail)) {
                throw new RuntimeException("Không có quyền truy cập session này");
            }
            
            // Get statistics
            Map<String, Object> stats = learningService.getSessionStatistics(sessionId);
            
            // Debug logging
            log.info("📊 Session Results - SessionID: {}", sessionId);
            log.info("   - Session from DB: targetWords={}, actualWords={}, correct={}, wrong={}, skip={}, time={}",
                session.getTargetWords(), session.getActualWords(), 
                session.getCorrectCount(), session.getWrongCount(), 
                session.getSkipCount(), session.getTimeSpentSec());
            log.info("   - Stats Map: {}", stats);
            log.info("   - Result DTO: totalWords={}, correct={}, wrong={}, skip={}",
                result.getTotalWords(), result.getCorrectCount(), 
                result.getWrongCount(), result.getSkipCount());

            model.addAttribute("result", result);
            model.addAttribute("stats", stats);
            model.addAttribute("pageTitle", "Kết quả học tập");

            return "learn/results";

        } catch (RuntimeException e) {
            log.error("Error loading session results", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/vocabulary/dictionaries";
        }
    }

    /**
     * Helper method to get current user ID from Authentication
     */
    private String getCurrentUserId(Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("Không tìm thấy thông tin người dùng");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserPrincipal customUser) {
            return customUser.getEmail();
        }
        if (principal instanceof OidcUser oidcUser) {
            return oidcUser.getEmail();
        }
        if (principal instanceof OAuth2User oAuth2User) {
            return (String) oAuth2User.getAttributes().getOrDefault("email", "");
        }

        return authentication.getName();
    }

    private String getSafeUserEmail(Authentication authentication) {
        try {
            return authentication != null ? getCurrentUserId(authentication) : "anonymous";
        } catch (RuntimeException ex) {
            return "unknown";
        }
    }
}
