package com.englishvocab.security;

import com.englishvocab.entity.User;
import com.englishvocab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthUserProvisioningService {

    private final UserRepository userRepository;

    @Transactional
    public User provisionUser(String email, String displayName, boolean emailVerified, String providerId) {
        if (!StringUtils.hasText(email)) {
            throw new OAuth2AuthenticationException("Email not found from OAuth provider");
        }

        if (!emailVerified) {
            throw new OAuth2AuthenticationException("Email is not verified by OAuth provider");
        }

        String normalizedEmail = email.trim();

        return userRepository.findByEmail(normalizedEmail)
                .map(existing -> linkExistingUser(existing, providerId))
                .orElseGet(() -> createGoogleUser(normalizedEmail, displayName, providerId));
    }

    private User linkExistingUser(User user, String providerId) {
        if (!user.isGoogleUser()) {
            log.debug("Linking existing user {} to Google account", user.getEmail());
            user.setGoogleUser(true);
        }

        if (StringUtils.hasText(providerId) && !providerId.equals(user.getGoogleId())) {
            user.setGoogleId(providerId);
        }

        return user;
    }

    private User createGoogleUser(String email, String displayName, String providerId) {
        String finalName = StringUtils.hasText(displayName)
                ? displayName.trim()
                : extractNameFromEmail(email);

        User user = User.builder()
                .username(email)
                .password(null)
                .fullname(finalName)
                .email(email)
                .googleUser(true)
                .googleId(providerId)
                .role(User.Role.USER)
                .status(User.Status.ACTIVE)
                .build();

        return userRepository.save(user);
    }

    private String extractNameFromEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "User";
        }
        String localPart = email.substring(0, atIndex);
        return localPart.substring(0, 1).toUpperCase() + localPart.substring(1);
    }
}
