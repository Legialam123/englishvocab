package com.englishvocab.controller;

import com.englishvocab.entity.User;
import com.englishvocab.entity.Dictionary;
import com.englishvocab.entity.Vocab;
import com.englishvocab.entity.UserCustomVocab;
import com.englishvocab.entity.UserVocabProgress;
import com.englishvocab.service.DictionaryService;
import com.englishvocab.service.VocabularyService;
import com.englishvocab.service.UserCustomVocabService;
import com.englishvocab.service.UserProgressService;
import com.englishvocab.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Controller quản lý từ vựng cho user
 */
@Controller
@RequestMapping("/vocabulary")
@RequiredArgsConstructor
@Slf4j
public class VocabularyController {
    
    private final DictionaryService dictionaryService;
    private final VocabularyService vocabularyService;
    private final UserCustomVocabService userCustomVocabService;
    private final UserProgressService userProgressService;
    
    /**
     * Trang chính - Danh sách từ vựng đã học
     */
    @GetMapping
    public String index(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String filter,
            Authentication authentication,
            Model model) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            Pageable pageable = PageRequest.of(page, size);
            
            // Lấy từ vựng đã học của user
            Page<UserVocabProgress> learnedWords = userProgressService.findUserProgress(currentUser, pageable);
            
            // Lấy từ vựng cá nhân của user
            List<UserCustomVocab> customWords = userCustomVocabService.findByUser(currentUser);
            
            // Statistics
            long totalLearned = userProgressService.countLearnedWords(currentUser);
            long totalCustom = userCustomVocabService.countByUser(currentUser);
            long wordsToReview = userProgressService.countWordsForReview(currentUser);
            
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("learnedWords", learnedWords);
            model.addAttribute("customWords", customWords);
            model.addAttribute("totalLearned", totalLearned);
            model.addAttribute("totalCustom", totalCustom);
            model.addAttribute("wordsToReview", wordsToReview);
            
            log.info("User {} accessed vocabulary page with {} learned words", 
                    currentUser.getUsername(), totalLearned);
            
            return "vocabulary/index";
            
        } catch (Exception e) {
            log.error("Error loading vocabulary page", e);
            model.addAttribute("errorMessage", "Lỗi khi tải danh sách từ vựng: " + e.getMessage());
            return "vocabulary/index";
        }
    }
    
    /**
     * Trang chọn từ điển để học
     */
    @GetMapping("/dictionaries")
    public String dictionaries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Authentication authentication,
            Model model) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            
            // Lấy từ điển active
            List<Dictionary> activeDictionaries = dictionaryService.findActiveDictionaries();
            
            // Statistics cho từng từ điển
            // TODO: Implement dictionary progress statistics
            
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("dictionaries", activeDictionaries);
            
            log.info("User {} browsing dictionaries", currentUser.getUsername());
            
            return "vocabulary/dictionaries";
            
        } catch (Exception e) {
            log.error("Error loading dictionaries page", e);
            model.addAttribute("errorMessage", "Lỗi khi tải danh sách từ điển: " + e.getMessage());
            return "vocabulary/dictionaries";
        }
    }
    
    /**
     * Xem từ vựng trong một từ điển
     */
    @GetMapping("/dictionary/{dictionaryId}")
    public String viewDictionary(
            @PathVariable Integer dictionaryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String search,
            Authentication authentication,
            Model model) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            Dictionary dictionary = dictionaryService.findByIdOrThrow(dictionaryId);
            Pageable pageable = PageRequest.of(page, size);
            
            // Filter setup
            Vocab.Level levelFilter = null;
            if (level != null && !level.isEmpty()) {
                try {
                    levelFilter = Vocab.Level.valueOf(level.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid level filter: {}", level);
                }
            }
            
            // Get vocabulary with filters
            Page<Vocab> vocabularies;
            if (search != null && !search.trim().isEmpty()) {
                List<Vocab> searchResults = vocabularyService.searchByWordInDictionary(dictionaryId, search.trim());
                model.addAttribute("vocabularies", searchResults);
                model.addAttribute("isSearch", true);
                model.addAttribute("searchKeyword", search.trim());
            } else {
                vocabularies = vocabularyService.findAllWithFilter(dictionaryId, levelFilter, pageable);
                model.addAttribute("vocabularies", vocabularies);
                model.addAttribute("isSearch", false);
            }
            
            // Get user's progress for these words
            // TODO: Add progress indicators for each word
            
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("dictionary", dictionary);
            model.addAttribute("levels", Vocab.Level.values());
            model.addAttribute("selectedLevel", level);
            
            log.info("User {} viewing dictionary: {} ({})", 
                    currentUser.getUsername(), dictionary.getName(), dictionaryId);
            
            return "vocabulary/dictionary";
            
        } catch (Exception e) {
            log.error("Error loading dictionary vocabulary", e);
            model.addAttribute("errorMessage", "Lỗi khi tải từ vựng: " + e.getMessage());
            return "redirect:/vocabulary/dictionaries";
        }
    }
    
    /**
     * Form thêm từ vựng cá nhân
     */
    @GetMapping("/add")
    public String addForm(Authentication authentication, Model model) {
        User currentUser = getCurrentUser(authentication);
        
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("customVocabForm", new CustomVocabForm());
        
        log.info("User {} accessing add custom vocabulary form", currentUser.getUsername());
        
        return "vocabulary/add";
    }
    
    /**
     * Lưu từ vựng cá nhân
     */
    @PostMapping("/add")
    public String addCustomVocab(
            @Valid @ModelAttribute("customVocabForm") CustomVocabForm form,
            BindingResult result,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            
            if (result.hasErrors()) {
                model.addAttribute("currentUser", currentUser);
                return "vocabulary/add";
            }
            
            // Check if word already exists for this user
            if (userCustomVocabService.existsByUserAndWord(currentUser, form.getWord())) {
                result.rejectValue("word", "error.word", "Bạn đã thêm từ này rồi");
                model.addAttribute("currentUser", currentUser);
                return "vocabulary/add";
            }
            
            // Create custom vocabulary
            UserCustomVocab customVocab = UserCustomVocab.builder()
                    .user(currentUser)
                    .name(form.getWord())
                    .ipa(form.getIpa())
                    .pos(form.getPos())
                    .meaningVi(form.getMeaningVi())
                    .build();
            
            userCustomVocabService.save(customVocab);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Đã thêm từ vựng '" + form.getWord() + "' thành công!");
            
            log.info("User {} added custom vocabulary: {}", currentUser.getUsername(), form.getWord());
            
            return "redirect:/vocabulary";
            
        } catch (Exception e) {
            log.error("Error adding custom vocabulary", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Lỗi khi thêm từ vựng: " + e.getMessage());
            return "redirect:/vocabulary/add";
        }
    }
    
    /**
     * Xem chi tiết từ vựng
     */
    @GetMapping("/{vocabId}")
    public String viewVocab(
            @PathVariable Integer vocabId,
            @RequestParam(required = false, defaultValue = "system") String type,
            Authentication authentication,
            Model model) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            
            if ("custom".equals(type)) {
                // Custom vocabulary
                UserCustomVocab customVocab = userCustomVocabService.findByIdOrThrow(vocabId);
                
                // Check ownership
                if (!customVocab.getUser().getId().equals(currentUser.getId())) {
                    log.warn("User {} tried to access custom vocab {} belonging to user {}", 
                            currentUser.getUsername(), vocabId, customVocab.getUser().getUsername());
                    return "redirect:/vocabulary";
                }
                
                model.addAttribute("customVocab", customVocab);
                model.addAttribute("isCustom", true);
                
            } else {
                // System vocabulary
                Vocab vocab = vocabularyService.findByIdOrThrow(vocabId);
                
                // Get user's progress for this word
                UserVocabProgress progress = userProgressService.findUserProgress(currentUser, vocab);
                
                model.addAttribute("vocab", vocab);
                model.addAttribute("progress", progress);
                model.addAttribute("isCustom", false);
            }
            
            model.addAttribute("currentUser", currentUser);
            
            return "vocabulary/detail";
            
        } catch (Exception e) {
            log.error("Error loading vocabulary detail", e);
            model.addAttribute("errorMessage", "Lỗi khi tải chi tiết từ vựng: " + e.getMessage());
            return "redirect:/vocabulary";
        }
    }
    
    /**
     * Xóa từ vựng cá nhân
     */
    @PostMapping("/custom/{customVocabId}/delete")
    public String deleteCustomVocab(
            @PathVariable Integer customVocabId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            UserCustomVocab customVocab = userCustomVocabService.findByIdOrThrow(customVocabId);
            
            // Check ownership
            if (!customVocab.getUser().getId().equals(currentUser.getId())) {
                log.warn("User {} tried to delete custom vocab {} belonging to user {}", 
                        currentUser.getUsername(), customVocabId, customVocab.getUser().getUsername());
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền xóa từ vựng này");
                return "redirect:/vocabulary";
            }
            
            String wordName = customVocab.getName();
            userCustomVocabService.delete(customVocabId);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Đã xóa từ vựng '" + wordName + "' thành công!");
            
            log.info("User {} deleted custom vocabulary: {}", currentUser.getUsername(), wordName);
            
        } catch (Exception e) {
            log.error("Error deleting custom vocabulary", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Lỗi khi xóa từ vựng: " + e.getMessage());
        }
        
        return "redirect:/vocabulary";
    }
    
    /**
     * Helper method to get current user from authentication
     */
    private User getCurrentUser(Authentication authentication) {
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
        
        User user = new User();
        user.setId(principal.getId());
        user.setUsername(principal.getUsername());
        user.setFullname(principal.getFullname());
        user.setEmail(principal.getEmail());
        user.setRole(principal.getRole());
        user.setStatus(principal.getStatus());
        
        return user;
    }
    
    // ===== FORM CLASSES =====
    
    /**
     * Form cho thêm từ vựng cá nhân
     */
    public static class CustomVocabForm {
        @NotBlank(message = "Từ vựng không được để trống")
        @Size(max = 100, message = "Từ vựng không được vượt quá 100 ký tự")
        private String word;
        
        @Size(max = 100, message = "IPA không được vượt quá 100 ký tự")
        private String ipa;
        
        @Size(max = 20, message = "Từ loại không được vượt quá 20 ký tự")
        private String pos;
        
        @Size(max = 50, message = "Nghĩa tiếng Việt không được vượt quá 50 ký tự")
        private String meaningVi;
        
        public CustomVocabForm() {}
        
        // Getters and setters
        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }
        public String getIpa() { return ipa; }
        public void setIpa(String ipa) { this.ipa = ipa; }
        public String getPos() { return pos; }
        public void setPos(String pos) { this.pos = pos; }
        public String getMeaningVi() { return meaningVi; }
        public void setMeaningVi(String meaningVi) { this.meaningVi = meaningVi; }
    }
}
