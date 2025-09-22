package com.englishvocab.security;

import com.englishvocab.entity.User;
import com.englishvocab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    
    private final UserRepository userRepository;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        
        try {
            log.info("🚀 Starting OAuth2 user loading process...");
            
            // Lấy thông tin user từ Google
            OAuth2User oauth2User = delegate.loadUser(userRequest);
            log.info("✅ Successfully loaded OAuth2 user from Google");
            
            // Extract user info từ Google
            String email = oauth2User.getAttribute("email");
            String name = oauth2User.getAttribute("name");
            String googleId = oauth2User.getAttribute("sub");
            
            log.info("📋 Google OAuth2 User Info:");
            log.info("   📧 Email: {}", email);
            log.info("   👤 Name: {}", name);
            log.info("   🆔 Google ID: {}", googleId);
            
            if (email == null || email.trim().isEmpty()) {
                log.error("❌ Email is null or empty from OAuth2 provider");
                throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
            }
            
            log.info("🔍 Checking if user exists in database with email: {}", email);
            
            // Tìm user trong database theo email
            User user = userRepository.findByEmail(email).orElse(null);
            
            if (user != null) {
                // OPTION A: Auto-link existing user
                log.info("🔗 AUTO-LINKING: Found existing user with ID: {}, Username: {}", 
                        user.getId(), user.getUsername());
                return createCustomOAuth2User(user, oauth2User.getAttributes());
            } else {
                // Tạo user mới từ Google
                log.info("👨‍💻 CREATING NEW USER: No existing user found, creating new Google user");
                User newUser = createUserFromOAuth2(email, name, googleId);
                return createCustomOAuth2User(newUser, oauth2User.getAttributes());
            }
            
        } catch (Exception e) {
            log.error("❌ Error during OAuth2 user loading: ", e);
            throw new OAuth2AuthenticationException("Failed to process OAuth2 user: " + e.getMessage());
        }
    }
    
    /**
     * Tạo user mới từ thông tin Google OAuth2
     */
    private User createUserFromOAuth2(String email, String name, String googleId) {
        try {
            String finalName = (name != null && !name.trim().isEmpty()) 
                ? name.trim() 
                : extractNameFromEmail(email);
                
            log.info("📝 Building new Google user:");
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
            
            log.info("🎉 SUCCESS: Created new Google user!");
            log.info("   🆔 User ID: {}", savedUser.getId());
            log.info("   📧 Email: {}", savedUser.getEmail());
            log.info("   👤 Username: {}", savedUser.getUsername());
            log.info("   🔍 Is Google User: {}", savedUser.isGoogleUser());
            
            return savedUser;
            
        } catch (Exception e) {
            log.error("❌ FAILED to create Google user for email: {}", email, e);
            throw new RuntimeException("Failed to create Google user: " + e.getMessage(), e);
        }
    }
    
    /**
     * Tạo OAuth2UserPrincipal từ User entity
     */
    private OAuth2UserPrincipal createCustomOAuth2User(User user, Map<String, Object> attributes) {
        return new OAuth2UserPrincipal(
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
                attributes
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
