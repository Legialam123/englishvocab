package com.englishvocab.security;

import com.englishvocab.entity.User;
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
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    
    private final OAuthUserProvisioningService provisioningService;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        
        try {
            OAuth2User oauth2User = delegate.loadUser(userRequest);

            String email = oauth2User.getAttribute("email");
            if (!StringUtils.hasText(email)) {
                throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
            }

            String displayName = oauth2User.getAttribute("name");
            String providerId = oauth2User.getAttribute("sub");

            User user = provisioningService.provisionUser(email, displayName, true, providerId);

            return createCustomOAuth2User(user, oauth2User.getAttributes());
        } catch (Exception e) {
            log.error("Failed to process OAuth2 user", e);
            throw new OAuth2AuthenticationException("Failed to process OAuth2 user: " + e.getMessage());
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
}
