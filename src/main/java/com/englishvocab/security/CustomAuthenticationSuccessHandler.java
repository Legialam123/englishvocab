package com.englishvocab.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                      HttpServletResponse response,
                                      Authentication authentication) throws IOException {
        
        log.info("Authentication successful for user: {}", authentication.getName());
        
        // Check if user has ADMIN role
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            log.info("Admin user detected, redirecting to dictionary management");
            response.sendRedirect("/admin/dictionaries");
        } else {
            log.info("Regular user detected, redirecting to dashboard");
            response.sendRedirect("/dashboard");
        }
    }
}
