package com.englishvocab.controller.admin;

import com.englishvocab.entity.Vocab;
import com.englishvocab.entity.Dictionary;
import com.englishvocab.entity.Topics;
import com.englishvocab.entity.Senses;
import com.englishvocab.service.VocabularyService;
import com.englishvocab.service.DictionaryService;
import com.englishvocab.service.TopicsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/vocabulary")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminVocabularyController {
    
    private final VocabularyService vocabularyService;
    private final DictionaryService dictionaryService;
    private final TopicsService topicsService;
    
    /**
     * Trang danh sách từ vựng
     */
    @GetMapping
    public String index(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer dictionaryId,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String search,
            Model model) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Vocab.Level levelFilter = null;
            
            if (level != null && !level.isEmpty()) {
                levelFilter = Vocab.Level.valueOf(level.toUpperCase());
            }
            
            Page<Vocab> vocabularies;
            if (search != null && !search.trim().isEmpty()) {
                // Search mode
                List<Vocab> searchResults;
                if (dictionaryId != null) {
                    searchResults = vocabularyService.searchByWordInDictionary(dictionaryId, search.trim());
                } else {
                    searchResults = vocabularyService.searchByWord(search.trim());
                }
                model.addAttribute("vocabularies", searchResults);
                model.addAttribute("isSearch", true);
                model.addAttribute("searchKeyword", search.trim());
            } else {
                // Normal pagination mode
                vocabularies = vocabularyService.findAllWithFilter(dictionaryId, levelFilter, pageable);
                model.addAttribute("vocabularies", vocabularies);
                model.addAttribute("isSearch", false);
            }
            
            // Statistics
            VocabularyService.VocabStats stats = vocabularyService.getStatistics();
            model.addAttribute("stats", stats);
            
            // Filter options
            List<Dictionary> dictionaries = dictionaryService.findActiveDictionaries();
            model.addAttribute("dictionaries", dictionaries);
            model.addAttribute("levelOptions", Vocab.Level.values());
            model.addAttribute("selectedDictionary", dictionaryId);
            model.addAttribute("selectedLevel", level);
            
            // Page title and navigation
            model.addAttribute("pageTitle", "Quản lý từ vựng");
            model.addAttribute("activeSection", "vocabulary");
            
        } catch (Exception e) {
            log.error("Lỗi khi load danh sách từ vựng", e);
            model.addAttribute("errorMessage", "Có lỗi xảy ra khi tải danh sách từ vựng");
            return "error/500";
        }
        
        return "admin/vocabulary/index";
    }
    
    /**
     * Trang tạo từ vựng mới
     */
    @GetMapping("/create")
    public String createForm(
            @RequestParam(required = false) Integer dictionaryId,
            Model model) {
        
        VocabCreateForm form = new VocabCreateForm();
        if (dictionaryId != null) {
            form.setDictionaryId(dictionaryId);
        }
        
        model.addAttribute("vocabForm", form);
        model.addAttribute("pageTitle", "Thêm từ vựng mới");
        model.addAttribute("levelOptions", Vocab.Level.values());
        model.addAttribute("posOptions", getPartOfSpeechOptions());
        model.addAttribute("activeSection", "vocabulary");
        
        List<Dictionary> dictionaries = dictionaryService.findActiveDictionaries();
        model.addAttribute("dictionaries", dictionaries);
        model.addAttribute("selectedDictionaryId", dictionaryId);
        
        // Add active topics for selection
        List<Topics> activeTopics = topicsService.findActiveTopics();
        model.addAttribute("activeTopics", activeTopics);
        
        return "admin/vocabulary/create";
    }
    
    /**
     * Xử lý tạo từ vựng
     */
    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("vocabForm") VocabCreateForm vocabForm,
                        @RequestParam(required = false) List<Integer> topicIds,
                        BindingResult bindingResult,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Thêm từ vựng mới");
            model.addAttribute("levelOptions", Vocab.Level.values());
            model.addAttribute("posOptions", getPartOfSpeechOptions());
            model.addAttribute("activeSection", "vocabulary");
            List<Dictionary> dictionaries = dictionaryService.findActiveDictionaries();
            model.addAttribute("dictionaries", dictionaries);
            List<Topics> activeTopics = topicsService.findActiveTopics();
            model.addAttribute("activeTopics", activeTopics);
            model.addAttribute("selectedDictionaryId", vocabForm.getDictionaryId());
            return "admin/vocabulary/create";
        }
        
        try {
            // Create vocab with primary sense
            Vocab saved = vocabularyService.createWithSense(vocabForm.toVocab(), vocabForm.toPrimarySense(null));
            
            // Assign topics if any selected
            if (topicIds != null && !topicIds.isEmpty()) {
                vocabularyService.assignTopics(saved.getVocabId(), topicIds);
                log.info("Assigned {} topics to vocab: {}", topicIds.size(), saved.getWord());
            }
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Đã thêm từ vựng '" + saved.getWord() + "' cùng với nghĩa thành công!");
            return "redirect:/admin/vocabulary";
            
        } catch (RuntimeException e) {
            log.error("Lỗi khi tạo từ vựng", e);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("pageTitle", "Thêm từ vựng mới");
            model.addAttribute("levelOptions", Vocab.Level.values());
            model.addAttribute("posOptions", getPartOfSpeechOptions());
            model.addAttribute("activeSection", "vocabulary");
            List<Dictionary> dictionaries = dictionaryService.findActiveDictionaries();
            model.addAttribute("dictionaries", dictionaries);
            List<Topics> activeTopics = topicsService.findActiveTopics();
            model.addAttribute("activeTopics", activeTopics);
            model.addAttribute("selectedDictionaryId", vocabForm.getDictionaryId());
            return "admin/vocabulary/create";
        }
    }
    
    /**
     * Trang chỉnh sửa từ vựng
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Vocab vocab = vocabularyService.findByIdOrThrow(id);
            model.addAttribute("vocab", vocab);
            model.addAttribute("pageTitle", "Chỉnh sửa từ vựng: " + vocab.getWord());
            model.addAttribute("levelOptions", Vocab.Level.values());   
            
            List<Dictionary> dictionaries = dictionaryService.findActiveDictionaries();
            model.addAttribute("dictionaries", dictionaries);

            return "admin/vocabulary/edit";
            
            
        } catch (RuntimeException e) {
            log.error("Không tìm thấy từ vựng với ID: {}", id);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/vocabulary";
        }
    }
    
    /**
     * Xử lý cập nhật từ vựng
     */
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Integer id,
                        @Valid @ModelAttribute Vocab vocab,
                        BindingResult bindingResult,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Chỉnh sửa từ vựng");
            model.addAttribute("levelOptions", Vocab.Level.values());
            List<Dictionary> dictionaries = dictionaryService.findActiveDictionaries();
            model.addAttribute("dictionaries", dictionaries);
            return "admin/vocabulary/edit";
        }
        
        try {
            Vocab updated = vocabularyService.update(id, vocab);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Đã cập nhật từ vựng '" + updated.getWord() + "' thành công!");
            return "redirect:/admin/vocabulary";
            
        } catch (RuntimeException e) {
            log.error("Lỗi khi cập nhật từ vựng", e);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("pageTitle", "Chỉnh sửa từ vựng");
            model.addAttribute("levelOptions", Vocab.Level.values());
            List<Dictionary> dictionaries = dictionaryService.findActiveDictionaries();
            model.addAttribute("dictionaries", dictionaries);
            return "admin/vocabulary/edit";
        }
    }
    
    /**
     * Xem chi tiết từ vựng
     */
    @GetMapping("/{id}")
    public String show(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Vocab vocab = vocabularyService.findByIdOrThrow(id);
            model.addAttribute("vocab", vocab);
            model.addAttribute("pageTitle", "Chi tiết từ vựng: " + vocab.getWord());
            model.addAttribute("activeSection", "vocabulary");
            
            // Note: vocab.senses and vocab.vocabTopics are loaded via JPA relationships
            log.info("Loaded vocabulary '{}' with {} senses", vocab.getWord(), 
                    vocab.getSenses() != null ? vocab.getSenses().size() : 0);
            
            return "admin/vocabulary/show";
            
        } catch (RuntimeException e) {
            log.error("Không tìm thấy từ vựng với ID: {}", id);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/vocabulary";
        }
    }
    
    /**
     * Xóa từ vựng
     */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            Vocab vocab = vocabularyService.findByIdOrThrow(id);
            String word = vocab.getWord();
            
            vocabularyService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Đã xóa từ vựng '" + word + "' thành công!");
            
        } catch (RuntimeException e) {
            log.error("Lỗi khi xóa từ vựng", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:/admin/vocabulary";
    }
    
    /**
     * Trang quản lý từ vựng theo dictionary
     */
    @GetMapping("/dictionary/{dictionaryId}")
    public String vocabularyByDictionary(
            @PathVariable Integer dictionaryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        try {
            Dictionary dictionary = dictionaryService.findByIdOrThrow(dictionaryId);
            Pageable pageable = PageRequest.of(page, size);
            Page<Vocab> vocabularies = vocabularyService.findByDictionary(dictionaryId, pageable);
            
            // Statistics for this dictionary
            VocabularyService.VocabStatsByDictionary stats = 
                vocabularyService.getStatisticsByDictionary(dictionaryId);
            
            model.addAttribute("dictionary", dictionary);
            model.addAttribute("vocabularies", vocabularies);
            model.addAttribute("stats", stats);
            model.addAttribute("pageTitle", "Từ vựng: " + dictionary.getName());
            
            return "admin/vocabulary/by-dictionary";
            
        } catch (RuntimeException e) {
            log.error("Lỗi khi load từ vựng theo dictionary", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/vocabulary";
        }
    }
    
    /**
     * DTO for creating vocabulary with senses
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class VocabCreateForm {
        // Vocab fields
        @jakarta.validation.constraints.NotBlank(message = "Từ vựng không được để trống")
        @jakarta.validation.constraints.Size(max = 100, message = "Từ vựng không được vượt quá 100 ký tự")
        private String word;
        
        @jakarta.validation.constraints.NotBlank(message = "Từ loại không được để trống")
        @jakarta.validation.constraints.Size(max = 20, message = "Từ loại không được vượt quá 20 ký tự")
        private String pos;
        
        @jakarta.validation.constraints.Size(max = 100, message = "Phiên âm không được vượt quá 100 ký tự")
        private String ipa;
        
        @jakarta.validation.constraints.NotNull(message = "Phải chọn level")
        private Vocab.Level level;
        
        @jakarta.validation.constraints.NotNull(message = "Phải chọn từ điển")
        private Integer dictionaryId;
        
        // Primary sense fields
        @jakarta.validation.constraints.NotBlank(message = "Nghĩa tiếng Việt không được để trống")
        @jakarta.validation.constraints.Size(max = 50, message = "Nghĩa tiếng Việt không được vượt quá 50 ký tự")
        private String meaningVi;
        
        @jakarta.validation.constraints.Size(max = 100, message = "Định nghĩa không được vượt quá 100 ký tự")
        private String definition;
        
        /**
         * Convert to Vocab entity
         */
        public Vocab toVocab() {
            Dictionary dictionary = new Dictionary();
            dictionary.setDictionaryId(this.dictionaryId);
            
            return Vocab.builder()
                    .word(this.word)
                    .pos(this.pos)
                    .ipa(this.ipa)
                    .level(this.level)
                    .dictionary(dictionary)
                    .build();
        }
        
        /**
         * Create primary sense for vocab
         */
        public Senses toPrimarySense(Vocab vocab) {
            return Senses.builder()
                    .vocab(vocab)
                    .meaningVi(this.meaningVi)
                    .definition(this.definition)
                    .build();
        }
    }
    
    /**
     * Get Part of Speech options
     */
    private java.util.Map<String, String> getPartOfSpeechOptions() {
        java.util.Map<String, String> options = new java.util.LinkedHashMap<>();
        options.put("noun", "Danh từ (noun)");
        options.put("verb", "Động từ (verb)");
        options.put("adjective", "Tính từ (adjective)");
        options.put("adverb", "Trạng từ (adverb)");
        options.put("pronoun", "Đại từ (pronoun)");
        options.put("preposition", "Giới từ (preposition)");
        options.put("conjunction", "Liên từ (conjunction)");
        options.put("interjection", "Thán từ (interjection)");
        return options;
    }
}
