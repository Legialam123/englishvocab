package com.englishvocab.controller.admin;

import com.englishvocab.entity.Topics;
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
@RequestMapping("/admin/topics")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminTopicsController {
    
    private final TopicsService topicsService;
    
    /**
     * Trang danh sách chủ đề
     */
    @GetMapping
    public String index(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Model model) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Topics.Status statusFilter = null;
            
            if (status != null && !status.isEmpty()) {
                statusFilter = Topics.Status.valueOf(status.toUpperCase());
            }
            
            Page<Topics> topics;
            if (search != null && !search.trim().isEmpty()) {
                // Search mode
                List<Topics> searchResults = topicsService.searchByName(search.trim());
                model.addAttribute("topics", searchResults);
                model.addAttribute("isSearch", true);
                model.addAttribute("searchKeyword", search.trim());
            } else {
                // Normal pagination mode
                topics = topicsService.findAllWithPagination(statusFilter, pageable);
                model.addAttribute("topics", topics);
                model.addAttribute("isSearch", false);
            }
            
            // Statistics
            TopicsService.TopicStats stats = topicsService.getStatistics();
            model.addAttribute("stats", stats);
            
            // Top topics with vocabulary count - temporarily comment out
            // List<TopicsService.TopicWithVocabCount> topTopics = topicsService.getTopicsWithVocabCount(5);
            // model.addAttribute("topTopics", topTopics);
            
            // Filter options
            model.addAttribute("statusOptions", Topics.Status.values());
            model.addAttribute("selectedStatus", status);
            
            // Page title and navigation
            model.addAttribute("pageTitle", "Quản lý chủ đề");
            model.addAttribute("activeSection", "topics");
            
        } catch (Exception e) {
            log.error("Lỗi khi load danh sách chủ đề", e);
            model.addAttribute("errorMessage", "Có lỗi xảy ra khi tải danh sách chủ đề");
            return "error/500";
        }
        
        return "admin/topics/index";
    }
    
    /**
     * Trang tạo chủ đề mới
     */
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("topic", new Topics());
        model.addAttribute("pageTitle", "Tạo chủ đề mới");
        model.addAttribute("statusOptions", Topics.Status.values());
        model.addAttribute("activeSection", "topics");
        return "admin/topics/create";
    }
    
    /**
     * Xử lý tạo chủ đề
     */
    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("topic") Topics topic,
                        BindingResult bindingResult,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Tạo chủ đề mới");
            model.addAttribute("statusOptions", Topics.Status.values());
            return "admin/topics/create";
        }
        
        try {
            Topics saved = topicsService.create(topic);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Đã tạo chủ đề '" + saved.getName() + "' thành công!");
            return "redirect:/admin/topics";
            
        } catch (RuntimeException e) {
            log.error("Lỗi khi tạo chủ đề", e);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("pageTitle", "Tạo chủ đề mới");
            model.addAttribute("statusOptions", Topics.Status.values());
            return "admin/topics/create";
        }
    }
    
    /**
     * Trang chỉnh sửa chủ đề
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Topics topic = topicsService.findByIdOrThrow(id);
            model.addAttribute("topic", topic);
            model.addAttribute("pageTitle", "Chỉnh sửa chủ đề: " + topic.getName());
            model.addAttribute("statusOptions", Topics.Status.values());
            return "admin/topics/edit";
            
        } catch (RuntimeException e) {
            log.error("Không tìm thấy chủ đề với ID: {}", id);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/topics";
        }
    }
    
    /**
     * Xử lý cập nhật chủ đề
     */
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Integer id,
                        @Valid @ModelAttribute("topic") Topics topic,
                        BindingResult bindingResult,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Chỉnh sửa chủ đề");
            model.addAttribute("statusOptions", Topics.Status.values());
            return "admin/topics/edit";
        }
        
        try {
            Topics updated = topicsService.update(id, topic);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Đã cập nhật chủ đề '" + updated.getName() + "' thành công!");
            return "redirect:/admin/topics";
            
        } catch (RuntimeException e) {
            log.error("Lỗi khi cập nhật chủ đề", e);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("pageTitle", "Chỉnh sửa chủ đề");
            model.addAttribute("statusOptions", Topics.Status.values());
            return "admin/topics/edit";
        }
    }
    
    /**
     * Xem chi tiết chủ đề
     */
    @GetMapping("/{id}")
    public String show(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Topics topic = topicsService.findByIdOrThrow(id);
            model.addAttribute("topic", topic);
            model.addAttribute("pageTitle", "Chi tiết chủ đề: " + topic.getName());
            model.addAttribute("activeSection", "topics");
            
            // Note: topic.vocabTopics are loaded via JPA relationships
            log.info("Loaded topic '{}' with {} vocabularies", topic.getName(), 
                    topic.getVocabTopics() != null ? topic.getVocabTopics().size() : 0);
            
            return "admin/topics/show";
            
        } catch (RuntimeException e) {
            log.error("Không tìm thấy chủ đề với ID: {}", id);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/topics";
        }
    }
    
    /**
     * Xóa chủ đề
     */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            Topics topic = topicsService.findByIdOrThrow(id);
            String name = topic.getName();
            
            topicsService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Đã xóa chủ đề '" + name + "' thành công!");
            
        } catch (RuntimeException e) {
            log.error("Lỗi khi xóa chủ đề", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:/admin/topics";
    }
    
    /**
     * Toggle status chủ đề
     */
    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            Topics topic = topicsService.findByIdOrThrow(id);
            String oldStatus = topic.getStatus().name();
            
            Topics.Status newStatus = (topic.getStatus() == Topics.Status.ACTIVE) 
                ? Topics.Status.INACTIVE 
                : Topics.Status.ACTIVE;
                
            topic.setStatus(newStatus);
            topicsService.update(topic.getTopicId(), topic);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Đã chuyển trạng thái chủ đề từ " + oldStatus + " sang " + newStatus.name());
            
        } catch (RuntimeException e) {
            log.error("Lỗi khi toggle status chủ đề", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:/admin/topics";
    }
}
