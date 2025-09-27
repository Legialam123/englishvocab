package com.englishvocab.controller;

import com.englishvocab.dto.AuthRequest;
import com.englishvocab.dto.RegisterRequest;
import com.englishvocab.entity.User;
import com.englishvocab.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final UserService userService;
    
    // ========== WEB ENDPOINTS (Thymeleaf) ==========
    
    /**
     * Trang đăng nhập
     */
    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("authRequest", new AuthRequest());
        return "auth/login";
    }
    
    /**
     * Trang đăng ký
     */
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }
    
    /**
     * Xử lý đăng nhập (Web)
     */
    @PostMapping("/login")
    public String login(@Valid @ModelAttribute AuthRequest request,
                       BindingResult bindingResult,
                       HttpSession session,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            return "auth/login";
        }
        
        try {
            User user = userService.login(request);
            session.setAttribute("currentUser", user);
            
            redirectAttributes.addFlashAttribute("successMessage", "Đăng nhập thành công!");
            
            // Redirect based on role
            if (user.getRole() == User.Role.ADMIN) {
                return "redirect:/admin/dictionaries";
            } else {
                return "redirect:/dashboard";
            }
            
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/login";
        }
    }
    
    /**
     * Xử lý đăng ký (Web)
     */
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterRequest request,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        
        try {
            User user = userService.register(request);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/auth/login";
            
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";
        }
    }
    
    /**
     * Đăng xuất
     */
    @PostMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("successMessage", "Đã đăng xuất thành công!");
        return "redirect:/auth/login";
    }
    
    // ========== REST API ENDPOINTS (JSON) ==========
    
    /**
     * API đăng nhập
     */
    @PostMapping("/api/login")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> loginApi(@Valid @RequestBody AuthRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User user = userService.login(request);
            response.put("success", true);
            response.put("message", "Đăng nhập thành công");
            response.put("user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "fullname", user.getFullname(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "status", user.getStatus()
            ));
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * API đăng ký
     */
    @PostMapping("/api/register")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registerApi(@Valid @RequestBody RegisterRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User user = userService.register(request);
            response.put("success", true);
            response.put("message", "Đăng ký thành công");
            response.put("user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "fullname", user.getFullname(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "status", user.getStatus()
            ));
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * API kiểm tra trạng thái đăng nhập
     */
    @GetMapping("/api/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAuthStatus(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        User currentUser = (User) session.getAttribute("currentUser");
        
        if (currentUser != null) {
            response.put("authenticated", true);
            response.put("user", Map.of(
                "id", currentUser.getId(),
                "username", currentUser.getUsername(),
                "fullname", currentUser.getFullname(),
                "email", currentUser.getEmail(),
                "role", currentUser.getRole(),
                "status", currentUser.getStatus()
            ));
        } else {
            response.put("authenticated", false);
        }
        
        return ResponseEntity.ok(response);
    }
}
