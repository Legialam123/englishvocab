package com.englishvocab.controller.admin;

import com.englishvocab.entity.User;
import com.englishvocab.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Controller quản lý người dùng cho Admin
 */
@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminUserController {
    
    private final UserService userService;
    
    /**
     * Danh sách người dùng với phân trang và filter
     */
    @GetMapping
    public String index(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) User.Role roleFilter,
            @RequestParam(required = false) User.Status statusFilter,
            @RequestParam(required = false) String keyword,
            Model model) {
        
        try {
            // Tạo Pageable với sort
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // Lấy dữ liệu với filter
            Page<User> users = userService.findAllWithPagination(roleFilter, statusFilter, keyword, pageable);
            
            // Thống kê
            UserService.UserStats stats = userService.getStatistics();
            
            // Recent users
            List<User> recentUsers = userService.getRecentUsers(5);
            
            // Add model attributes
            model.addAttribute("activeSection", "users");
            model.addAttribute("users", users);
            model.addAttribute("stats", stats);
            model.addAttribute("recentUsers", recentUsers);
            
            // Filter options
            model.addAttribute("roleOptions", User.Role.values());
            model.addAttribute("statusOptions", User.Status.values());
            
            // Current filters
            model.addAttribute("selectedRole", roleFilter);
            model.addAttribute("selectedStatus", statusFilter);
            model.addAttribute("keyword", keyword);
            
            // Pagination
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", users.getTotalPages());
            model.addAttribute("totalElements", users.getTotalElements());
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDir", sortDir);
            
            log.info("Loaded users page with {} users", users.getTotalElements());
            return "admin/users/index";
            
        } catch (Exception e) {
            log.error("Error loading users page", e);
            model.addAttribute("errorMessage", "Lỗi khi tải danh sách người dùng: " + e.getMessage());
            return "admin/users/index";
        }
    }
    
    /**
     * Xem chi tiết người dùng
     */
    @GetMapping("/{id}")
    public String view(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByIdOrThrow(id);
            
            model.addAttribute("activeSection", "users");
            model.addAttribute("user", user);
            
            return "admin/users/view";
            
        } catch (RuntimeException e) {
            log.error("User not found with id: {}", id);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/users";
        }
    }
    
    /**
     * Form chỉnh sửa người dùng
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByIdOrThrow(id);
            
            model.addAttribute("activeSection", "users");
            model.addAttribute("user", user);
            model.addAttribute("roleOptions", User.Role.values());
            model.addAttribute("statusOptions", User.Status.values());
            
            return "admin/users/edit";
            
        } catch (RuntimeException e) {
            log.error("User not found for edit with id: {}", id);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/users";
        }
    }
    
    /**
     * Cập nhật người dùng
     */
    @PostMapping("/{id}/edit")
    public String update(@PathVariable String id,
                        @Valid @ModelAttribute User user,
                        BindingResult bindingResult,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeSection", "users");
            model.addAttribute("roleOptions", User.Role.values());
            model.addAttribute("statusOptions", User.Status.values());
            return "admin/users/edit";
        }
        
        try {
            User updatedUser = userService.adminUpdateUser(id, user);
            
            redirectAttributes.addFlashAttribute("successMessage",
                "Đã cập nhật thông tin người dùng '" + updatedUser.getUsername() + "' thành công");
            
            return "redirect:/admin/users/" + updatedUser.getId();
            
        } catch (RuntimeException e) {
            log.error("Error updating user with id: {}", id, e);
            
            model.addAttribute("activeSection", "users");
            model.addAttribute("roleOptions", User.Role.values());
            model.addAttribute("statusOptions", User.Status.values());
            model.addAttribute("errorMessage", e.getMessage());
            
            return "admin/users/edit";
        }
    }
    
    /**
     * Thay đổi role người dùng
     */
    @PostMapping("/{id}/change-role")
    public String changeRole(@PathVariable String id,
                            @RequestParam User.Role newRole,
                            RedirectAttributes redirectAttributes) {
        try {
            User updated = userService.changeRole(id, newRole);
            
            redirectAttributes.addFlashAttribute("successMessage",
                "Đã thay đổi quyền của '" + updated.getUsername() + "' thành: " + 
                (newRole == User.Role.ADMIN ? "Quản trị viên" : "Người dùng"));
            
        } catch (RuntimeException e) {
            log.error("Error changing role for user: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:/admin/users/" + id;
    }
    
    /**
     * Thay đổi trạng thái người dùng
     */
    @PostMapping("/{id}/change-status")
    public String changeStatus(@PathVariable String id,
                              @RequestParam User.Status newStatus,
                              RedirectAttributes redirectAttributes) {
        try {
            User updated = userService.changeStatus(id, newStatus);
            
            String statusText = switch(newStatus) {
                case ACTIVE -> "Hoạt động";
                case LOCKED -> "Bị khóa";
                case DELETED -> "Đã xóa";
            };
            
            redirectAttributes.addFlashAttribute("successMessage",
                "Đã thay đổi trạng thái của '" + updated.getUsername() + "' thành: " + statusText);
            
        } catch (RuntimeException e) {
            log.error("Error changing status for user: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:/admin/users/" + id;
    }
    
    /**
     * Xóa người dùng (soft delete)
     */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByIdOrThrow(id); // Get user info before deletion
            userService.adminDeleteUser(id);
            
            redirectAttributes.addFlashAttribute("successMessage",
                "Đã xóa người dùng '" + user.getUsername() + "' thành công");
            
        } catch (RuntimeException e) {
            log.error("Error deleting user: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:/admin/users";
    }
    
    /**
     * Reset password người dùng
     */
    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable String id,
                               @RequestParam String newPassword,
                               RedirectAttributes redirectAttributes) {
        try {
            if (newPassword == null || newPassword.trim().length() < 6) {
                throw new RuntimeException("Mật khẩu mới phải có ít nhất 6 ký tự");
            }
            
            User user = userService.findByIdOrThrow(id);
            user.setPassword(newPassword); // Will be encoded in service
            userService.adminUpdateUser(id, user);
            
            redirectAttributes.addFlashAttribute("successMessage",
                "Đã reset mật khẩu cho '" + user.getUsername() + "' thành công");
            
        } catch (RuntimeException e) {
            log.error("Error resetting password for user: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:/admin/users/" + id;
    }
}

