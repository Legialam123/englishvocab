package com.englishvocab.service;

import com.englishvocab.dto.AuthRequest;
import com.englishvocab.dto.RegisterRequest;
import com.englishvocab.entity.User;
import com.englishvocab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    
    /**
     * Đăng ký user mới
     */
    public User register(RegisterRequest request) {
        log.info("Đăng ký user mới với username: {}", request.getUsername());
        
        // Kiểm tra username đã tồn tại
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username đã tồn tại");
        }
        
        // Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }
        
        // Kiểm tra password và confirmPassword khớp
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Password và xác nhận password không khớp");
        }
        
        // Tạo user mới
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullname(request.getFullname())
                .email(request.getEmail())
                .role(User.Role.USER)
                .status(User.Status.ACTIVE)
                .build();
        
        User savedUser = userRepository.save(user);
        log.info("Đăng ký thành công user với ID: {}", savedUser.getId());
        return savedUser;
    }
    
    /**
     * Đăng nhập
     */
    public User login(AuthRequest request) {
        log.info("Đăng nhập với username/email: {}", request.getUsernameOrEmail());
        
        try {
            // Xác thực với Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsernameOrEmail(),
                            request.getPassword()
                    )
            );
            
            // Lấy thông tin user
            String username = authentication.getName();
            User user = findByUsernameOrEmail(username, username)
                    .orElseThrow(() -> new RuntimeException("User không tồn tại"));
                    
            log.info("Đăng nhập thành công user với ID: {}", user.getId());
            return user;
            
        } catch (AuthenticationException e) {
            log.error("Đăng nhập thất bại: {}", e.getMessage());
            throw new RuntimeException("Username/Email hoặc Password không đúng");
        }
    }
    
    /**
     * Tìm user theo username hoặc email
     */
    @Transactional(readOnly = true)
    public Optional<User> findByUsernameOrEmail(String username, String email) {
        return userRepository.findByUsernameOrEmail(username, email);
    }
    
    /**
     * Tìm user theo ID
     */
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    
    /**
     * Lấy tất cả users
     */
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }
    
    /**
     * Tìm users theo status
     */
    @Transactional(readOnly = true)
    public List<User> findByStatus(User.Status status) {
        return userRepository.findByStatus(status);
    }
    
    /**
     * Cập nhật thông tin user
     */
    public User updateUser(Long id, User updatedUser) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setFullname(updatedUser.getFullname());
                    user.setEmail(updatedUser.getEmail());
                    if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                        user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
                    }
                    return userRepository.save(user);
                })
                .orElseThrow(() -> new RuntimeException("User không tồn tại với ID: " + id));
    }
    
    /**
     * Cập nhật status user
     */
    public User updateUserStatus(Long id, User.Status status) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setStatus(status);
                    return userRepository.save(user);
                })
                .orElseThrow(() -> new RuntimeException("User không tồn tại với ID: " + id));
    }
    
    /**
     * Xóa user (soft delete bằng cách set status = DELETED)
     */
    public void deleteUser(Long id) {
        updateUserStatus(id, User.Status.DELETED);
        log.info("Đã xóa user với ID: {}", id);
    }
}
