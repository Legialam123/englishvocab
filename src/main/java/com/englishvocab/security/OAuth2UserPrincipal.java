package com.englishvocab.security;

import com.englishvocab.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

@Getter
public class OAuth2UserPrincipal extends CustomUserPrincipal implements OAuth2User {
    
    private final Map<String, Object> attributes;
    
    public OAuth2UserPrincipal(String id, String username, String password, String fullname,
                             String email, User.Role role, User.Status status,
                             Collection<? extends GrantedAuthority> authorities,
                             Map<String, Object> attributes) {
        super(id, username, password, fullname, email, role, status, authorities);
        this.attributes = attributes;
    }
    
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
    
    @Override
    public String getName() {
        // Google OAuth2 sử dụng 'sub' làm name attribute
        return (String) attributes.get("sub");
    }
}
