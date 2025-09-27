package com.englishvocab.security;

import com.englishvocab.entity.User;
import com.englishvocab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOidcUserService extends OidcUserService {
    
    private final UserRepository userRepository;
    
    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        
        try {
            log.info("🚀 Starting OIDC user loading process...");
            
            // Lấy thông tin user từ Google OIDC
            OidcUser oidcUser = super.loadUser(userRequest);
            log.info("Successfully loaded OIDC user from Google");
            
            // Extract user info từ Google OIDC
            String email = oidcUser.getEmail();
            String name = oidcUser.getFullName();
            String googleId = oidcUser.getSubject();
            
            log.info("📋 Google OIDC User Info:");
            log.info("   📧 Email: {}", email);
            log.info("   👤 Name: {}", name);
            log.info("   🆔 Google Subject: {}", googleId);
            log.info("   ✅ Email Verified: {}", oidcUser.getEmailVerified());
            
            if (email == null || email.trim().isEmpty()) {
                log.error("❌ Email is null or empty from OIDC provider");
                throw new OAuth2AuthenticationException("Email not found from OIDC provider");
            }
            
            log.info("🔍 Checking if user exists in database with email: {}", email);
            
            // Tìm user trong database theo email
            User user = userRepository.findByEmail(email).orElse(null);
            
            if (user != null) {
                // OPTION A: Auto-link existing user
                log.info("🔗 AUTO-LINKING: Found existing user with ID: {}, Username: {}", 
                        user.getId(), user.getUsername());
                return createCustomOidcUser(user, oidcUser);
            } else {
                // Tạo user mới từ Google
                log.info("👨‍💻 CREATING NEW USER: No existing user found, creating new Google user");
                User newUser = createUserFromOidc(email, name, googleId);
                return createCustomOidcUser(newUser, oidcUser);
            }
            
        } catch (Exception e) {
            log.error("❌ Error during OIDC user loading: ", e);
            throw new OAuth2AuthenticationException("Failed to process OIDC user: " + e.getMessage());
        }
    }
    
    /**
     * Tạo user mới từ thông tin Google OIDC
     */
    private User createUserFromOidc(String email, String name, String googleId) {
        try {
            String finalName = (name != null && !name.trim().isEmpty()) 
                ? name.trim() 
                : extractNameFromEmail(email);
                
            log.info("📝 Building new Google OIDC user:");
            log.info("   📧 Username (email): {}", email);
            log.info("   👤 Fullname: {}", finalName);
            log.info("   🔑 Password: null (Google user)");
            log.info("   👥 Role: USER");
            log.info("   ✅ Status: ACTIVE");
            
            User user = User.builder()
                    .username(email.trim())  // Dùng email làm username
                    .password(null)   // Không có password cho Google user
                    .fullname(finalName)
                    .email(email.trim())
                    .role(User.Role.USER)  // Mặc định role USER
                    .status(User.Status.ACTIVE)  // Active ngay
                    .build();
            
            log.info("💾 Saving user to database...");
            User savedUser = userRepository.save(user);
            
            log.info("🎉 SUCCESS: Created new Google OIDC user!");
            log.info("   🆔 User ID: {}", savedUser.getId());
            log.info("   📧 Email: {}", savedUser.getEmail());
            log.info("   👤 Username: {}", savedUser.getUsername());
            log.info("   🔍 Is Google User: {}", savedUser.isGoogleUser());
            
            return savedUser;
            
        } catch (Exception e) {
            log.error("❌ FAILED to create Google OIDC user for email: {}", email, e);
            throw new RuntimeException("Failed to create Google OIDC user: " + e.getMessage(), e);
        }
    }
    
    /**
     * Tạo custom OIDC user principal
     */
    private OidcUserPrincipal createCustomOidcUser(User user, OidcUser oidcUser) {
        return new OidcUserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getFullname(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                ),
                oidcUser.getAttributes(),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo()
        );
    }
    
    /**
     * Extract name từ email nếu không có name từ Google
     */
    private String extractNameFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "User";
        }
        String localPart = email.split("@")[0];
        // Capitalize first letter
        return localPart.substring(0, 1).toUpperCase() + localPart.substring(1);
    }
}
