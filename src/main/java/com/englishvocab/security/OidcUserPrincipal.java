package com.englishvocab.security;

import com.englishvocab.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.Map;

@Getter
public class OidcUserPrincipal extends CustomUserPrincipal implements OidcUser {
    
    private final Map<String, Object> attributes;
    private final OidcIdToken idToken;
    private final OidcUserInfo userInfo;
    
    public OidcUserPrincipal(Long id, String username, String password, String fullname,
                           String email, User.Role role, User.Status status,
                           Collection<? extends GrantedAuthority> authorities,
                           Map<String, Object> attributes,
                           OidcIdToken idToken, OidcUserInfo userInfo) {
        super(id, username, password, fullname, email, role, status, authorities);
        this.attributes = attributes;
        this.idToken = idToken;
        this.userInfo = userInfo;
    }
    
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
    
    @Override
    public String getName() {
        // OIDC sử dụng 'sub' (subject) làm name attribute
        return idToken.getSubject();
    }
    
    @Override
    public OidcIdToken getIdToken() {
        return idToken;
    }
    
    @Override
    public OidcUserInfo getUserInfo() {
        return userInfo;
    }
    
    @Override
    public Map<String, Object> getClaims() {
        return idToken.getClaims();
    }
}
