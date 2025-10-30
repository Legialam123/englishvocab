package com.englishvocab.controller;

import com.englishvocab.dto.DashboardStatsDTO;
import com.englishvocab.dto.VocabListSummaryDTO;
import com.englishvocab.entity.User;
import com.englishvocab.security.CustomUserPrincipal;
import com.englishvocab.service.DictionaryService;
import com.englishvocab.service.MediaService;
import com.englishvocab.service.UserVocabListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;


@Controller
@Slf4j
@RequiredArgsConstructor
public class DashboardController {
    
    private final UserVocabListService userVocabListService;
    private final DictionaryService dictionaryService;
    private final MediaService mediaService;
    
    /**
     * Trang chủ - Dashboard  
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        
        if (currentUser != null) {
            log.info("Dashboard accessed by user: {} ({})", currentUser.getUsername(), currentUser.getRole());
            model.addAttribute("currentUser", currentUser);
            
            // Add avatar URL
            String avatarUrl = mediaService.getUserAvatarUrl(currentUser.getId());
            model.addAttribute("avatarUrl", avatarUrl);
            
            // Add role-specific data to model
            boolean isAdmin = currentUser.getRole() == User.Role.ADMIN;
            model.addAttribute("isAdmin", isAdmin);
            
            // Get dashboard statistics
            DashboardStatsDTO stats = userVocabListService.getDashboardStats(currentUser);
            model.addAttribute("stats", stats);
            
            // Get popular dictionaries with stats (top 6 active dictionaries)
            var popularDictionaries = dictionaryService.getActiveDictionariesWithStats();
            if (popularDictionaries.size() > 6) {
                popularDictionaries = popularDictionaries.subList(0, 6);
            }
            model.addAttribute("dictionaries", popularDictionaries);
            
            // Get recent lists (top 5)
            List<VocabListSummaryDTO> recentLists = userVocabListService.getRecentLists(currentUser, 5);
            model.addAttribute("recentLists", recentLists);
            
            // Get recently learned vocabulary from progress (top 12)
            var recentlyLearned = userVocabListService.getRecentlyLearnedVocabulary(currentUser, 12);
            model.addAttribute("recentlyLearned", recentlyLearned);
            
            log.info("Dashboard data loaded: {} lists, {} dictionaries, {} recently learned", 
                    recentLists.size(), popularDictionaries.size(), recentlyLearned.size());
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
