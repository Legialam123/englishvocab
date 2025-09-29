package com.englishvocab.controller;

import com.englishvocab.entity.Dictionary;
import com.englishvocab.entity.Topics;
import com.englishvocab.service.DictionaryService;
import com.englishvocab.service.LearningService;
import com.englishvocab.service.TopicsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * Controller xử lý các chức năng học từ vựng
 * 
 * Learning Modes:
 * 1. Alphabetical - Học theo thứ tự A-Z
 * 2. Topics - Học theo chủ đề 
 * 3. Custom - Tự chọn từ vựng
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
    private final LearningService learningService;

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

            // TODO: Implement alphabetical vocabulary loading
            // Pageable pageable = PageRequest.of(page - 1, size);
            // Page<Vocab> vocabularies = vocabularyService.findByDictionaryAlphabetical(
            //     dictionaryId, startLetter, level, pageable);

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

            // TODO: Load topics for this dictionary with vocab counts
            // List<TopicWithVocabCount> topics = topicsService.getTopicsWithVocabCountForDictionary(dictionaryId);

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

            // TODO: Load vocabulary with advanced filtering
            // Pageable pageable = PageRequest.of(page - 1, size);
            // Page<Vocab> vocabularies = vocabularyService.findByDictionaryWithFilters(
            //     dictionaryId, search, level, topicIds, pageable);

            // Load available topics for filtering
            List<Topics> availableTopics = topicsService.findActiveTopics();
            model.addAttribute("availableTopics", availableTopics);

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
            String currentUserId = getCurrentUserId(authentication);

            // TODO: Create learning session
            // String sessionId = learningService.createLearningSession(
            //     currentUserId, dictionaryId, learningMode, selectedVocabIds, topicIds, sessionSize);

            log.info("User {} started learning session for dictionary {} in {} mode", 
                    currentUserId, dictionaryId, learningMode);

            // Redirect to flashcard session
            return "redirect:/learn/session/flashcards?sessionId=" + "temp-session-id";

        } catch (RuntimeException e) {
            log.error("Error starting learning session", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/vocabulary/dictionary/" + dictionaryId;
        }
    }

    /**
     * 🃏 FLASHCARD SESSION
     * Interface học với flashcards
     */
    @GetMapping("/session/flashcards")
    public String flashcardSession(
            @RequestParam String sessionId,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            String currentUserId = getCurrentUserId(authentication);

            // TODO: Load learning session
            // LearningSession session = learningService.getSession(sessionId, currentUserId);
            
            model.addAttribute("pageTitle", "Học từ vựng - Flashcards");
            // model.addAttribute("session", session);

            log.info("User {} accessing flashcard session {}", currentUserId, sessionId);

            return "learn/flashcards";

        } catch (RuntimeException e) {
            log.error("Error loading flashcard session", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/vocabulary/dictionaries";
        }
    }

    /**
     * 📊 SESSION COMPLETE
     * Hoàn thành session và hiển thị kết quả
     */
    @PostMapping("/session/complete")
    @ResponseBody
    public ResponseEntity<?> completeSession(
            @RequestBody SessionResultRequest request,
            Authentication authentication) {

        try {
            String currentUserId = getCurrentUserId(authentication);

            // TODO: Complete session and update progress
            // learningService.completeSession(request.getSessionId(), currentUserId, request);

            log.info("User {} completed learning session {} with {} answers in {} seconds", 
                    currentUserId, request.getSessionId(), 
                    request.getAnswers() != null ? request.getAnswers().size() : 0, 
                    request.getDuration());

            // Return success response for AJAX
            return ResponseEntity.ok().body(Map.of(
                "success", true,
                "message", "Session completed successfully",
                "redirectUrl", "/learn/session/results?sessionId=" + request.getSessionId()
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
            String currentUserId = getCurrentUserId(authentication);

            // TODO: Load session results
            // LearningSessionResult result = learningService.getSessionResult(sessionId, currentUserId);

            model.addAttribute("pageTitle", "Kết quả học tập");
            // model.addAttribute("result", result);

            log.info("User {} viewing results for session {}", currentUserId, sessionId);

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
        if (authentication.getPrincipal() instanceof OidcUser) {
            return ((OidcUser) authentication.getPrincipal()).getEmail();
        } else if (authentication.getPrincipal() instanceof OAuth2User) {
            return ((OAuth2User) authentication.getPrincipal()).getAttribute("email");
        } else {
            return authentication.getName();
        }
    }

    /**
     * DTO for session completion request
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SessionResultRequest {
        private String sessionId;
        private List<SessionAnswer> answers;
        private Integer duration; // in seconds
        private String completedAt;
    }

    /**
     * Enhanced DTO for individual vocabulary answer - 3-level system
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SessionAnswer {
        private Integer vocabId;
        private String answerLevel;  // 'mastered', 'temporary', 'unknown'
        private String timestamp;
        
        // Legacy support
        @Deprecated
        public boolean isCorrect() {
            return "mastered".equals(answerLevel) || "temporary".equals(answerLevel);
        }
    }
}
