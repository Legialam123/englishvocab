package com.englishvocab.security;

import com.englishvocab.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOidcUserService extends OidcUserService {
    
    private final OAuthUserProvisioningService provisioningService;
    
    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        
        try {
            OidcUser oidcUser = super.loadUser(userRequest);

            String email = oidcUser.getEmail();
            if (!StringUtils.hasText(email)) {
                throw new OAuth2AuthenticationException("Email not found from OIDC provider");
            }

            boolean emailVerified = Boolean.TRUE.equals(oidcUser.getEmailVerified());
            String displayName = oidcUser.getFullName();
            String providerId = oidcUser.getSubject();

            User user = provisioningService.provisionUser(email, displayName, emailVerified, providerId);

            return createCustomOidcUser(user, oidcUser);
        } catch (Exception e) {
            log.error("Failed to process OIDC user", e);
            throw new OAuth2AuthenticationException("Failed to process OIDC user: " + e.getMessage());
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
}
