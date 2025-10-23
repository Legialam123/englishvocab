package com.englishvocab.controller;

import com.englishvocab.dto.CreateListRequest;
import com.englishvocab.dto.UpdateListRequest;
import com.englishvocab.dto.VocabListSummaryDTO;
import com.englishvocab.entity.User;
import com.englishvocab.entity.UserVocabList;
import com.englishvocab.security.CustomUserPrincipal;
import com.englishvocab.service.UserVocabListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Controller xử lý các trang liên quan đến Vocabulary Lists
 */
@Controller
@RequestMapping("/vocabulary/lists")
@RequiredArgsConstructor
@Slf4j
public class VocabListPageController {
    
    private final UserVocabListService userVocabListService;
    
    /**
     * Trang danh sách các lists của user
     */
    @GetMapping
    public String myLists(Authentication authentication, Model model) {
        try {
            User currentUser = getCurrentUser(authentication);
            
            // Lấy tất cả lists với thống kê
            List<VocabListSummaryDTO> lists = userVocabListService.getListSummaries(currentUser);
            
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("lists", lists);
            
            return "vocabulary/list/my-lists";
            
        } catch (Exception e) {
            log.error("Error loading vocabulary lists", e);
            model.addAttribute("errorMessage", "Lỗi khi tải danh sách: " + e.getMessage());
            return "vocabulary/list/my-lists";
        }
    }
    
    /**
     * Form tạo list mới
     */
    @GetMapping("/create")
    public String createListForm(Authentication authentication, Model model) {
        User currentUser = getCurrentUser(authentication);
        
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("createListRequest", new CreateListRequest());
        
        return "vocabulary/list/create";
    }
    
    /**
     * Xử lý tạo list mới
     */
    @PostMapping("/create")
    public String createList(
            @Valid @ModelAttribute("createListRequest") CreateListRequest request,
            BindingResult result,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            
            if (result.hasErrors()) {
                model.addAttribute("currentUser", currentUser);
                return "vocabulary/list/create";
            }
            
            UserVocabList list = userVocabListService.createList(currentUser, request);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Đã tạo danh sách '" + list.getName() + "' thành công!");
            
            return "redirect:/vocabulary/lists/" + list.getUserVocabListId();
            
        } catch (Exception e) {
            log.error("Error creating vocabulary list", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Lỗi khi tạo danh sách: " + e.getMessage());
            return "redirect:/vocabulary/lists/create";
        }
    }
    
    /**
     * Trang chi tiết list
     */
    @GetMapping("/{listId}")
    public String viewList(
            @PathVariable Integer listId,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            
            UserVocabList list = userVocabListService.getListById(listId, currentUser);
            
            // Get vocabulary items in this list
            var vocabItems = userVocabListService.getAllVocabulary(listId, currentUser);
            
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("list", list);
            model.addAttribute("vocabItems", vocabItems);
            
            return "vocabulary/list/detail";
            
        } catch (Exception e) {
            log.error("Error loading vocabulary list", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Lỗi khi tải danh sách: " + e.getMessage());
            return "redirect:/vocabulary/lists";
        }
    }
    
    /**
     * Form chỉnh sửa list
     */
    @GetMapping("/{listId}/edit")
    public String editListForm(
            @PathVariable Integer listId,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            
            UserVocabList list = userVocabListService.getListById(listId, currentUser);
            
            UpdateListRequest request = UpdateListRequest.builder()
                    .name(list.getName())
                    .description(list.getDescription())
                    .visibility(list.getVisibility())
                    .status(list.getStatus())
                    .build();
            
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("list", list);
            model.addAttribute("updateListRequest", request);
            
            return "vocabulary/list/edit";
            
        } catch (Exception e) {
            log.error("Error loading edit form", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Lỗi khi tải form: " + e.getMessage());
            return "redirect:/vocabulary/lists";
        }
    }
    
    /**
     * Xử lý cập nhật list
     */
    @PostMapping("/{listId}/edit")
    public String updateList(
            @PathVariable Integer listId,
            @Valid @ModelAttribute("updateListRequest") UpdateListRequest request,
            BindingResult result,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            
            if (result.hasErrors()) {
                UserVocabList list = userVocabListService.getListById(listId, currentUser);
                model.addAttribute("currentUser", currentUser);
                model.addAttribute("list", list);
                return "vocabulary/list/edit";
            }
            
            userVocabListService.updateList(listId, request, currentUser);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Đã cập nhật danh sách thành công!");
            
            return "redirect:/vocabulary/lists/" + listId;
            
        } catch (Exception e) {
            log.error("Error updating vocabulary list", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Lỗi khi cập nhật: " + e.getMessage());
            return "redirect:/vocabulary/lists/" + listId + "/edit";
        }
    }
    
    /**
     * Xóa từ vựng hệ thống khỏi danh sách
     */
    @PostMapping("/{listId}/remove-system/{vocabId}")
    public String removeSystemVocab(
            @PathVariable Integer listId,
            @PathVariable Integer vocabId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            
            userVocabListService.removeSystemVocab(listId, vocabId, currentUser);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Đã xóa từ vựng khỏi danh sách!");
            
            return "redirect:/vocabulary/lists/" + listId;
            
        } catch (Exception e) {
            log.error("Error removing system vocab from list", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Lỗi khi xóa từ vựng: " + e.getMessage());
            return "redirect:/vocabulary/lists/" + listId;
        }
    }
    
    /**
     * Xóa từ vựng tùy chỉnh khỏi danh sách
     */
    @PostMapping("/{listId}/remove-custom/{customVocabId}")
    public String removeCustomVocab(
            @PathVariable Integer listId,
            @PathVariable Integer customVocabId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            
            userVocabListService.removeCustomVocab(listId, customVocabId, currentUser);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Đã xóa từ vựng khỏi danh sách!");
            
            return "redirect:/vocabulary/lists/" + listId;
            
        } catch (Exception e) {
            log.error("Error removing custom vocab from list", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Lỗi khi xóa từ vựng: " + e.getMessage());
            return "redirect:/vocabulary/lists/" + listId;
        }
    }
    
    /**
     * Xóa list
     */
    @PostMapping("/{listId}/delete")
    public String deleteList(
            @PathVariable Integer listId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            
            UserVocabList list = userVocabListService.getListById(listId, currentUser);
            String listName = list.getName();
            
            userVocabListService.deleteList(listId, currentUser);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Đã xóa danh sách '" + listName + "' thành công!");
            
            return "redirect:/vocabulary/lists";
            
        } catch (Exception e) {
            log.error("Error deleting vocabulary list", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Lỗi khi xóa danh sách: " + e.getMessage());
            return "redirect:/vocabulary/lists";
        }
    }
    
    /**
     * Helper method to get current user
     */
    private User getCurrentUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserPrincipal) {
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
        throw new RuntimeException("User not authenticated");
    }
}

