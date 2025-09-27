package com.englishvocab.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller cho trang chủ công khai
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class HomeController {

    /**
     * Trang chủ - Landing page
     */
    @GetMapping({"/", "/home"})
    public String home(Model model) {
        log.info("Serving landing page");
        
        // Thống kê demo cho landing page
        model.addAttribute("totalUsers", "1000+");
        model.addAttribute("totalWords", "10,000+");
        model.addAttribute("totalDictionaries", "50+");
        
        return "home/index";
    }
    
    /**
     * Trang giới thiệu
     */
    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("pageTitle", "Giới thiệu");
        return "home/about";
    }
    
    /**
     * Trang tính năng
     */
    @GetMapping("/features")
    public String features(Model model) {
        model.addAttribute("pageTitle", "Tính năng");
        return "home/features";
    }
}
