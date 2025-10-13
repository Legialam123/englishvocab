package com.englishvocab.service;

import com.englishvocab.dto.AuthRequest;
import com.englishvocab.dto.RegisterRequest;
import com.englishvocab.entity.User;
import com.englishvocab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public Optional<User> findById(String id) {
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
    public User updateUser(String id, User updatedUser) {
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
    public User updateUserStatus(String id, User.Status status) {
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
    public void deleteUser(String id) {
        updateUserStatus(id, User.Status.DELETED);
        log.info("Đã xóa user với ID: {}", id);
    }
    
    // ==================== ADMIN METHODS ====================
    
    /**
     * Tìm tất cả users với phân trang và filter
     */
    @Transactional(readOnly = true)
    public Page<User> findAllWithPagination(User.Role roleFilter, User.Status statusFilter, String keyword, Pageable pageable) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return userRepository.findByKeywordWithPagination(keyword.trim(), pageable);
        }
        
        if (roleFilter != null && statusFilter != null) {
            return userRepository.findByRoleAndStatus(roleFilter, statusFilter, pageable);
        } else if (roleFilter != null) {
            return userRepository.findByRole(roleFilter, pageable);
        } else if (statusFilter != null) {
            return userRepository.findByStatus(statusFilter, pageable);
        } else {
            return userRepository.findAll(pageable);
        }
    }
    
    /**
     * Tìm user theo ID (cho admin) - throw exception nếu không tồn tại
     */
    @Transactional(readOnly = true)
    public User findByIdOrThrow(String id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + id));
    }
    
    /**
     * Cập nhật user (admin only)
     */
    public User adminUpdateUser(String id, User updatedUser) {
        User existingUser = findByIdOrThrow(id);
        
        // Validate unique username (excluding current user)
        if (!existingUser.getUsername().equals(updatedUser.getUsername()) && 
            userRepository.existsByUsername(updatedUser.getUsername())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại");
        }
        
        // Validate unique email (excluding current user)  
        if (!existingUser.getEmail().equals(updatedUser.getEmail()) && 
            userRepository.existsByEmail(updatedUser.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }
        
        // Update fields
        existingUser.setUsername(updatedUser.getUsername());
        existingUser.setFullname(updatedUser.getFullname());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setRole(updatedUser.getRole());
        existingUser.setStatus(updatedUser.getStatus());
        
        // Only update password if provided
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().trim().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }
        
        existingUser.setUpdatedAt(java.time.LocalDateTime.now());
        User saved = userRepository.save(existingUser);
        
        log.info("Admin updated user: {}", saved.getUsername());
        return saved;
    }
    
    /**
     * Thay đổi role của user
     */
    public User changeRole(String userId, User.Role newRole) {
        User user = findByIdOrThrow(userId);
        User.Role oldRole = user.getRole();
        user.setRole(newRole);
        user.setUpdatedAt(java.time.LocalDateTime.now());
        
        User saved = userRepository.save(user);
        log.info("Admin changed role of user {} from {} to {}", user.getUsername(), oldRole, newRole);
        return saved;
    }
    
    /**
     * Thay đổi status của user
     */
    public User changeStatus(String userId, User.Status newStatus) {
        User user = findByIdOrThrow(userId);
        User.Status oldStatus = user.getStatus();
        user.setStatus(newStatus);
        user.setUpdatedAt(java.time.LocalDateTime.now());
        
        User saved = userRepository.save(user);
        log.info("Admin changed status of user {} from {} to {}", user.getUsername(), oldStatus, newStatus);
        return saved;
    }
    
    /**
     * Xóa user (admin only) - soft delete và validate
     */
    public void adminDeleteUser(String userId) {
        User user = findByIdOrThrow(userId);
        
        // Prevent deleting admin users
        if (user.getRole() == User.Role.ADMIN) {
            throw new RuntimeException("Không thể xóa tài khoản quản trị viên");
        }
        
        user.setStatus(User.Status.DELETED);
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
        
        log.info("Admin soft-deleted user: {}", user.getUsername());
    }
    
    /**
     * Thống kê user cho admin dashboard
     */
    @Transactional(readOnly = true)
    public UserStats getStatistics() {
        long total = userRepository.count();
        long active = userRepository.countByStatus(User.Status.ACTIVE);
        long locked = userRepository.countByStatus(User.Status.LOCKED);
        long deleted = userRepository.countByStatus(User.Status.DELETED);
        long admins = userRepository.countByRole(User.Role.ADMIN);
        long users = userRepository.countByRole(User.Role.USER);
        
        return UserStats.builder()
            .total(total)
            .active(active)
            .locked(locked)
            .deleted(deleted)
            .admins(admins)
            .users(users)
            .build();
    }
    
    /**
     * Lấy users mới đăng ký gần đây
     */
    @Transactional(readOnly = true)
    public List<User> getRecentUsers(int limit) {
        return userRepository.findRecentUsers(User.Status.DELETED, 
            org.springframework.data.domain.PageRequest.of(0, limit));
    }
    
    // ===== USER PROFILE MANAGEMENT =====
    
    /**
     * Cập nhật profile của user
     */
    @Transactional
    public User updateProfile(User user) {
        try {
            // Validate user exists
            User existingUser = findByIdOrThrow(user.getId());
            
            // Update allowed fields
            existingUser.setFullname(user.getFullname());
            existingUser.setEmail(user.getEmail());
            existingUser.setUpdatedAt(java.time.LocalDateTime.now());
            
            User updatedUser = userRepository.save(existingUser);
            log.info("Updated profile for user: {}", user.getUsername());
            
            return updatedUser;
            
        } catch (Exception e) {
            log.error("Error updating profile for user: {}", user.getUsername(), e);
            throw new RuntimeException("Không thể cập nhật profile: " + e.getMessage());
        }
    }
    
    /**
     * Đổi mật khẩu user
     */
    @Transactional
    public void changePassword(User user, String newPassword) {
        try {
            // Validate user exists
            User existingUser = findByIdOrThrow(user.getId());
            
            // Encode and update password
            String encodedPassword = passwordEncoder.encode(newPassword);
            existingUser.setPassword(encodedPassword);
            existingUser.setUpdatedAt(java.time.LocalDateTime.now());
            
            userRepository.save(existingUser);
            log.info("Changed password for user: {}", user.getUsername());
            
        } catch (Exception e) {
            log.error("Error changing password for user: {}", user.getUsername(), e);
            throw new RuntimeException("Không thể đổi mật khẩu: " + e.getMessage());
        }
    }
    
    /**
     * Cập nhật cài đặt học tập của user
     * TODO: Implement when user preferences are added to User entity
     */
    @Transactional
    public void updateLearningPreferences(User user, Integer dailyNewWords, Boolean emailNotifications) {
        try {
            // This will be implemented when we add preferences fields to User entity
            log.info("Learning preferences updated for user: {}", user.getUsername());
            
        } catch (Exception e) {
            log.error("Error updating learning preferences for user: {}", user.getUsername(), e);
            throw new RuntimeException("Không thể cập nhật cài đặt học tập: " + e.getMessage());
        }
    }
    
    /**
     * Stats DTO cho admin dashboard
     */
    @lombok.Data
    @lombok.Builder
    public static class UserStats {
        private long total;
        private long active;
        private long locked;
        private long deleted;
        private long admins;
        private long users;
    }
}
