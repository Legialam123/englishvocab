package com.englishvocab.security;

import com.englishvocab.entity.User;
import com.englishvocab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        log.info("Đang load user details cho: {}", usernameOrEmail);
        
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> {
                    log.error("User không tồn tại: {}", usernameOrEmail);
                    return new UsernameNotFoundException("User không tồn tại: " + usernameOrEmail);
                });
        
        // Kiểm tra trạng thái user
        if (user.getStatus() != User.Status.ACTIVE) {
            log.error("User không active: {} - Status: {}", usernameOrEmail, user.getStatus());
            throw new UsernameNotFoundException("Tài khoản đã bị vô hiệu hóa");
        }
        
        log.info("Load user details thành công cho: {} với role: {}", user.getUsername(), user.getRole());
        
        return CustomUserPrincipal.builder()
                .id(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .fullname(user.getFullname())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .authorities(Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                ))
                .build();
    }
}
