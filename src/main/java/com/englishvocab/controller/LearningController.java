package com.englishvocab.controller;

import com.englishvocab.dto.SessionResultDTO;
import com.englishvocab.dto.SessionResultRequest;
import com.englishvocab.entity.Dictionary;
import com.englishvocab.entity.LearningSession;
import com.englishvocab.entity.SessionVocabulary;
import com.englishvocab.entity.Topics;
import com.englishvocab.entity.User;
import com.englishvocab.entity.Vocab;
import com.englishvocab.security.CustomUserPrincipal;
import com.englishvocab.repository.UserRepository;
import com.englishvocab.service.DictionaryService;
import com.englishvocab.service.LearningService;
import com.englishvocab.service.TopicsService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

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

            // Load vocabulary alphabetically from database
            Pageable pageable = PageRequest.of(page - 1, size);
            Page<Vocab> vocabularies = vocabularyService.findByDictionaryOrderByWordAsc(dictionaryId, pageable);
            
            // Apply filters if provided
            if (startLetter != null && !startLetter.isEmpty()) {
                vocabularies = vocabularyService.findByDictionaryAndWordStartingWith(dictionaryId, startLetter, pageable);
            }
            
            if (level != null && !level.isEmpty()) {
                vocabularies = vocabularyService.findByDictionaryAndLevel(dictionaryId, level, pageable);
            }

            model.addAttribute("vocabularies", vocabularies.getContent());
            model.addAttribute("totalPages", vocabularies.getTotalPages());
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("startLetter", startLetter);
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

            // Load vocabulary with advanced filtering from database
            Pageable pageable = PageRequest.of(page - 1, size);
            Page<Vocab> vocabularies = vocabularyService.findByDictionaryWithFilters(
                dictionaryId, search, level, topicIds, pageable);

            // Load available topics for filtering
            List<Topics> availableTopics = topicsService.findActiveTopics();
            model.addAttribute("availableTopics", availableTopics);
            
            // Add vocabulary data to model
            model.addAttribute("vocabularies", vocabularies.getContent());
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
            @RequestParam(defaultValue = "20") int sessionSize,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            // Get current user
            String userEmail = getCurrentUserId(authentication);
            if (log.isDebugEnabled()) {
                log.debug("Start session request from user={} dictionaryId={} mode={} sessionSize={} selectedVocabIds={} topicIds={}",
                        userEmail, dictionaryId, learningMode, sessionSize, selectedVocabIds, topicIds);
            }

            User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
            
            // Get dictionary
            Dictionary dictionary = dictionaryService.findByIdOrThrow(dictionaryId);

            // Create learning session (cached in Redis)
            LearningSession session = learningService.createSession(
                user, dictionary, learningMode, selectedVocabIds, sessionSize);

            log.info("User {} started learning session {} for dictionary {} in {} mode", 
                    userEmail, session.getSessionUuid(), dictionary.getName(), learningMode);

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
            
            model.addAttribute("session", session);
            model.addAttribute("vocabularies", vocabularies.getContent());
            model.addAttribute("dictionary", session.getDictionary());
            model.addAttribute("pageTitle", "Học từ vựng - Flashcards");

            log.info("User {} accessing flashcard session {}", userEmail, sessionId);

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
