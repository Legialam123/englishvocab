package com.englishvocab.controller;

import com.englishvocab.entity.User;
import com.englishvocab.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class DashboardController {
    
    private final UserService userService;
    
    /**
     * Trang chủ - Dashboard
     */
    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        
        if (currentUser != null) {
            model.addAttribute("currentUser", currentUser);
            
            // Nếu là admin, hiển thị thống kê
            if (currentUser.getRole() == User.Role.ADMIN) {
                List<User> allUsers = userService.findAll();
                List<User> activeUsers = userService.findByStatus(User.Status.ACTIVE);
                
                model.addAttribute("totalUsers", allUsers.size());
                model.addAttribute("activeUsers", activeUsers.size());
                model.addAttribute("recentUsers", allUsers.stream()
                    .limit(5)
                    .toList());
            }
        }
        
        return "dashboard";
    }
    
    /**
     * Trang profile
     */
    @GetMapping("/profile")
    public String profile(Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser != null) {
            model.addAttribute("currentUser", currentUser);
        }
        return "profile";
    }
}
