package com.englishvocab.security;

import com.englishvocab.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomUserPrincipal implements UserDetails {
    
    private String id;
    private String username;
    private String password;
    private String fullname;
    private String email;
    private User.Role role;
    private User.Status status;
    private Collection<? extends GrantedAuthority> authorities;
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
    
    @Override
    public String getPassword() {
        return password;
    }
    
    @Override
    public String getUsername() {
        return username;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return status != User.Status.DELETED;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return status != User.Status.LOCKED;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return status == User.Status.ACTIVE;
    }
}
