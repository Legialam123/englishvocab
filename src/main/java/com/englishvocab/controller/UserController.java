package com.englishvocab.controller;

import com.englishvocab.entity.User;
import com.englishvocab.service.UserService;
import com.englishvocab.repository.UserRepository;
import com.englishvocab.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Controller quản lý profile và settings của user
 */
@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * Hiển thị trang profile
     */
    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        try {
            User currentUser = getCurrentUser(authentication);
            
            // Profile form objects
            model.addAttribute("user", currentUser);
            model.addAttribute("profileForm", new ProfileUpdateForm(currentUser));
            model.addAttribute("passwordForm", new PasswordChangeForm());
            model.addAttribute("preferencesForm", new PreferencesForm(currentUser));
            
            // Statistics
            // TODO: Add user learning statistics here
            model.addAttribute("totalWordsLearned", 0);
            model.addAttribute("currentStreak", 0);
            model.addAttribute("joinedDate", currentUser.getCreatedAt());
            
            log.info("User {} accessed profile page", currentUser.getUsername());
            return "user/profile";
            
        } catch (Exception e) {
            log.error("Error loading profile page", e);
            model.addAttribute("errorMessage", "Lỗi khi tải trang profile: " + e.getMessage());
            return "user/profile";
        }
    }
    
    /**
     * Cập nhật thông tin profile
     */
    @PostMapping("/profile/update")
    public String updateProfile(
            @Valid @ModelAttribute("profileForm") ProfileUpdateForm form,
            BindingResult result,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        try {
            if (result.hasErrors()) {
                User currentUser = getCurrentUser(authentication);
                model.addAttribute("user", currentUser);
                model.addAttribute("passwordForm", new PasswordChangeForm());
                model.addAttribute("preferencesForm", new PreferencesForm(currentUser));
                return "user/profile";
            }
            
            User currentUser = getCurrentUser(authentication);
            
            // Validate email uniqueness (if changed)
            if (!form.getEmail().equals(currentUser.getEmail())) {
                if (userRepository.existsByEmail(form.getEmail())) {
                    result.rejectValue("email", "error.email", "Email này đã được sử dụng bởi tài khoản khác");
                    
                    model.addAttribute("user", currentUser);
                    model.addAttribute("passwordForm", new PasswordChangeForm());
                    model.addAttribute("preferencesForm", new PreferencesForm(currentUser));
                    return "user/profile";
                }
            }
            
            // Update user info
            currentUser.setFullname(form.getFullname());
            currentUser.setEmail(form.getEmail());
            
            userService.updateProfile(currentUser);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Thông tin profile đã được cập nhật thành công!");
            log.info("User {} updated profile", currentUser.getUsername());
            
        } catch (Exception e) {
            log.error("Error updating profile", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Lỗi khi cập nhật profile: " + e.getMessage());
        }
        
        return "redirect:/user/profile";
    }
    
    /**
     * Đổi mật khẩu
     */
    @PostMapping("/password")
    public String changePassword(
            @Valid @ModelAttribute("passwordForm") PasswordChangeForm form,
            BindingResult result,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        try {
            if (result.hasErrors()) {
                User currentUser = getCurrentUser(authentication);
                model.addAttribute("user", currentUser);
                model.addAttribute("profileForm", new ProfileUpdateForm(currentUser));
                model.addAttribute("preferencesForm", new PreferencesForm(currentUser));
                return "user/profile";
            }
            
            User currentUser = getCurrentUser(authentication);
            
            // Verify current password
            if (!passwordEncoder.matches(form.getCurrentPassword(), currentUser.getPassword())) {
                result.rejectValue("currentPassword", "error.currentPassword", "Mật khẩu hiện tại không đúng");
                
                model.addAttribute("user", currentUser);
                model.addAttribute("profileForm", new ProfileUpdateForm(currentUser));
                model.addAttribute("preferencesForm", new PreferencesForm(currentUser));
                return "user/profile";
            }
            
            // Verify password confirmation
            if (!form.getNewPassword().equals(form.getConfirmPassword())) {
                result.rejectValue("confirmPassword", "error.confirmPassword", "Mật khẩu xác nhận không khớp");
                
                model.addAttribute("user", currentUser);
                model.addAttribute("profileForm", new ProfileUpdateForm(currentUser));
                model.addAttribute("preferencesForm", new PreferencesForm(currentUser));
                return "user/profile";
            }
            
            // Update password
            userService.changePassword(currentUser, form.getNewPassword());
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Mật khẩu đã được thay đổi thành công!");
            log.info("User {} changed password", currentUser.getUsername());
            
        } catch (Exception e) {
            log.error("Error changing password", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Lỗi khi đổi mật khẩu: " + e.getMessage());
        }
        
        return "redirect:/user/profile";
    }
    
    /**
     * Cập nhật cài đặt học tập
     */
    @PostMapping("/preferences")
    public String updatePreferences(
            @Valid @ModelAttribute("preferencesForm") PreferencesForm form,
            BindingResult result,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        try {
            if (result.hasErrors()) {
                User currentUser = getCurrentUser(authentication);
                model.addAttribute("user", currentUser);
                model.addAttribute("profileForm", new ProfileUpdateForm(currentUser));
                model.addAttribute("passwordForm", new PasswordChangeForm());
                return "user/profile";
            }
            
            User currentUser = getCurrentUser(authentication);
            
            // TODO: Update user learning preferences
            // This will be implemented when we add learning preferences to User entity
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Cài đặt học tập đã được cập nhật!");
            log.info("User {} updated learning preferences", currentUser.getUsername());
            
        } catch (Exception e) {
            log.error("Error updating preferences", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Lỗi khi cập nhật cài đặt: " + e.getMessage());
        }
        
        return "redirect:/user/profile";
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
        
        // Get full user details from database
        return userService.findByIdOrThrow(principal.getId());
    }
    
    // ===== FORM CLASSES =====
    
    /**
     * Form cho cập nhật profile
     */
    public static class ProfileUpdateForm {
        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 100, message = "Họ tên không được vượt quá 100 ký tự")
        private String fullname;
        
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        @Size(max = 100, message = "Email không được vượt quá 100 ký tự")
        private String email;
        
        public ProfileUpdateForm() {}
        
        public ProfileUpdateForm(User user) {
            this.fullname = user.getFullname();
            this.email = user.getEmail();
        }
        
        // Getters and setters
        public String getFullname() { return fullname; }
        public void setFullname(String fullname) { this.fullname = fullname; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
    
    /**
     * Form cho đổi mật khẩu
     */
    public static class PasswordChangeForm {
        @NotBlank(message = "Mật khẩu hiện tại không được để trống")
        private String currentPassword;
        
        @NotBlank(message = "Mật khẩu mới không được để trống")
        @Size(min = 6, message = "Mật khẩu mới phải có ít nhất 6 ký tự")
        private String newPassword;
        
        @NotBlank(message = "Xác nhận mật khẩu không được để trống")
        private String confirmPassword;
        
        // Getters and setters
        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    }
    
    /**
     * Form cho cài đặt học tập
     */
    public static class PreferencesForm {
        private Integer dailyNewWords = 10;
        private Boolean emailNotifications = true;
        private Boolean darkMode = false;
        private String interfaceLanguage = "vi";
        
        public PreferencesForm() {}
        
        public PreferencesForm(User user) {
            // TODO: Load from user preferences when implemented
        }
        
        // Getters and setters
        public Integer getDailyNewWords() { return dailyNewWords; }
        public void setDailyNewWords(Integer dailyNewWords) { this.dailyNewWords = dailyNewWords; }
        public Boolean getEmailNotifications() { return emailNotifications; }
        public void setEmailNotifications(Boolean emailNotifications) { this.emailNotifications = emailNotifications; }
        public Boolean getDarkMode() { return darkMode; }
        public void setDarkMode(Boolean darkMode) { this.darkMode = darkMode; }
        public String getInterfaceLanguage() { return interfaceLanguage; }
        public void setInterfaceLanguage(String interfaceLanguage) { this.interfaceLanguage = interfaceLanguage; }
    }
}
