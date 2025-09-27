package com.englishvocab.controller.admin;

import com.englishvocab.entity.Dictionary;
import com.englishvocab.entity.Vocab;
import com.englishvocab.service.DictionaryService;
import com.englishvocab.service.VocabularyService;
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
@RequestMapping("/admin/dictionaries")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminDictionaryController {
    
    private final DictionaryService dictionaryService;
    private final VocabularyService vocabularyService;
    
    /**
     * Trang danh sách từ điển
     */
    @GetMapping
    public String index(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Model model) {
        
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Dictionary.Status statusFilter = null;
            
            if (status != null && !status.isEmpty()) {
                statusFilter = Dictionary.Status.valueOf(status.toUpperCase());
            }
            
            Page<Dictionary> dictionaries;
            if (search != null && !search.trim().isEmpty()) {
                // Search mode - không dùng pagination cho simple search
                List<Dictionary> searchResults = dictionaryService.searchByName(search.trim());
                model.addAttribute("dictionaries", searchResults);
                model.addAttribute("isSearch", true);
                model.addAttribute("searchKeyword", search.trim());
            } else {
                // Normal pagination mode
                dictionaries = dictionaryService.findAllWithPagination(statusFilter, pageable);
                model.addAttribute("dictionaries", dictionaries);
                model.addAttribute("isSearch", false);
            }
            
            // Statistics
            DictionaryService.DictionaryStats stats = dictionaryService.getStatistics();
            model.addAttribute("stats", stats);
            
            // Filter options
            model.addAttribute("statusOptions", Dictionary.Status.values());
            model.addAttribute("selectedStatus", status);
            
            // Page title and navigation
            model.addAttribute("pageTitle", "Quản lý từ điển");
            model.addAttribute("activeSection", "dictionaries");
            
        } catch (Exception e) {
            log.error("Lỗi khi load danh sách từ điển", e);
            model.addAttribute("errorMessage", "Có lỗi xảy ra khi tải danh sách từ điển");
            return "error/500";
        }
        
        return "admin/dictionaries/index";
    }
    
    /**
     * Trang tạo từ điển mới
     */
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("dictionary", new Dictionary());
        model.addAttribute("pageTitle", "Tạo từ điển mới");
        model.addAttribute("statusOptions", Dictionary.Status.values());
        model.addAttribute("activeSection", "dictionaries");
        return "admin/dictionaries/create";
    }
    
    /**
     * Xử lý tạo từ điển
     */
    @PostMapping("/create")
    public String create(@Valid @ModelAttribute Dictionary dictionary,
                        BindingResult bindingResult,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Tạo từ điển mới");
            model.addAttribute("statusOptions", Dictionary.Status.values());
            return "admin/dictionaries/create";
        }
        
        try {
            Dictionary saved = dictionaryService.create(dictionary);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Đã tạo từ điển '" + saved.getName() + "' thành công!");
            return "redirect:/admin/dictionaries";
            
        } catch (RuntimeException e) {
            log.error("Lỗi khi tạo từ điển", e);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("pageTitle", "Tạo từ điển mới");
            model.addAttribute("statusOptions", Dictionary.Status.values());
            return "admin/dictionaries/create";
        }
    }
    
    /**
     * Trang chỉnh sửa từ điển
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Dictionary dictionary = dictionaryService.findByIdOrThrow(id);
            model.addAttribute("dictionary", dictionary);
            model.addAttribute("pageTitle", "Chỉnh sửa từ điển: " + dictionary.getName());
            model.addAttribute("statusOptions", Dictionary.Status.values());
            model.addAttribute("activeSection", "dictionaries");
            return "admin/dictionaries/edit";
            
        } catch (RuntimeException e) {
            log.error("Không tìm thấy từ điển với ID: {}", id);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/dictionaries";
        }
    }
    
    /**
     * Xử lý cập nhật từ điển
     */
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Integer id,
                        @Valid @ModelAttribute Dictionary dictionary,
                        BindingResult bindingResult,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Chỉnh sửa từ điển");
            model.addAttribute("statusOptions", Dictionary.Status.values());
            return "admin/dictionaries/edit";
        }
        
        try {
            Dictionary updated = dictionaryService.update(id, dictionary);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Đã cập nhật từ điển '" + updated.getName() + "' thành công!");
            return "redirect:/admin/dictionaries";
            
        } catch (RuntimeException e) {
            log.error("Lỗi khi cập nhật từ điển", e);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("pageTitle", "Chỉnh sửa từ điển");
            model.addAttribute("statusOptions", Dictionary.Status.values());
            return "admin/dictionaries/edit";
        }
    }
    
    /**
     * Xem chi tiết từ điển
     */
    @GetMapping("/{id}")
    public String show(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Dictionary dictionary = dictionaryService.findByIdOrThrow(id);
            model.addAttribute("dictionary", dictionary);
            model.addAttribute("pageTitle", "Chi tiết từ điển: " + dictionary.getName());
            model.addAttribute("activeSection", "dictionaries");
            
            // Load vocabulary statistics for this dictionary
            VocabularyService.VocabStatsByDictionary stats = 
                vocabularyService.getStatisticsByDictionary(id);
            model.addAttribute("stats", stats);
            
            // Load recent vocabularies (latest 10)
            Pageable recentPageable = PageRequest.of(0, 10);
            Page<Vocab> recentVocabPage = vocabularyService.findByDictionary(id, recentPageable);
            model.addAttribute("recentVocabularies", recentVocabPage.getContent());
            
            log.info("Dictionary {} loaded with {} vocabularies", dictionary.getName(), stats.getTotal());
            
            return "admin/dictionaries/show";
            
        } catch (RuntimeException e) {
            log.error("Không tìm thấy từ điển với ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/dictionaries";
        }
    }
    
    /**
     * Xóa từ điển
     */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            dictionaryService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa từ điển thành công!");
            
        } catch (RuntimeException e) {
            log.error("Lỗi khi xóa từ điển", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:/admin/dictionaries";
    }
    
    /**
     * Toggle status từ điển
     */
    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            Dictionary dictionary = dictionaryService.findByIdOrThrow(id);
            
            // Toggle between ACTIVE and INACTIVE
            Dictionary.Status newStatus = dictionary.getStatus() == Dictionary.Status.ACTIVE 
                ? Dictionary.Status.INACTIVE 
                : Dictionary.Status.ACTIVE;
                
            dictionary.setStatus(newStatus);
            dictionaryService.update(id, dictionary);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Đã chuyển trạng thái từ điển thành: " + 
                (newStatus == Dictionary.Status.ACTIVE ? "Hoạt động" : "Không hoạt động"));
            
        } catch (RuntimeException e) {
            log.error("Lỗi khi toggle status từ điển", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:/admin/dictionaries";
    }
}
