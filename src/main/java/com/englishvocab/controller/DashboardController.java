package com.englishvocab.controller;

import com.englishvocab.entity.User;
import com.englishvocab.security.CustomUserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
@Slf4j
public class DashboardController {
    
    /**
     * Trang chủ - Dashboard  
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        
        if (currentUser != null) {
            log.info("Dashboard accessed by user: {} ({})", currentUser.getUsername(), currentUser.getRole());
            model.addAttribute("currentUser", currentUser);
            
            // Add role-specific data to model
            if (currentUser.getRole() == User.Role.ADMIN) {
                // Admin can view dashboard but with admin-specific content
                log.info("Admin user viewing dashboard");
                model.addAttribute("isAdmin", true);
                // Could add admin stats here if needed
            } else {
                log.info("Regular user accessing dashboard");
                model.addAttribute("isAdmin", false);
            }
        } else {
            log.warn("Dashboard accessed but currentUser is null");
        }
        
        return "dashboard";
    }
    
    /**
     * Trang profile
     */
    @GetMapping("/profile")
    public String profile(Model model, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        if (currentUser != null) {
            log.info("Profile accessed by user: {}", currentUser.getUsername());
            model.addAttribute("currentUser", currentUser);
        } else {
            log.warn("Profile accessed but currentUser is null");
        }
        return "profile";
    }
    
    /**
     * Helper method để lấy current user từ Authentication context
     */
    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Authentication is null or not authenticated");
            return null;
        }
        
        Object principal = authentication.getPrincipal();
        log.debug("Principal type: {}", principal.getClass().getSimpleName());
        
        try {
            if (principal instanceof CustomUserPrincipal customUserPrincipal) {
                // All user principals (including OAuth2 and OIDC) extend CustomUserPrincipal
                // Build User entity from principal data
                return User.builder()
                    .id(customUserPrincipal.getId())
                    .username(customUserPrincipal.getUsername())
                    .password(customUserPrincipal.getPassword())
                    .fullname(customUserPrincipal.getFullname())
                    .email(customUserPrincipal.getEmail())
                    .role(customUserPrincipal.getRole())
                    .status(customUserPrincipal.getStatus())
                    .build();
            } else {
                log.warn("Unknown principal type: {}", principal.getClass().getName());
                return null;
            }
        } catch (Exception e) {
            log.error("Error extracting user from principal", e);
            return null;
        }
    }
}
