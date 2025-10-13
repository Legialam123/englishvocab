package com.englishvocab.repository;

import com.englishvocab.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    /**
     * Tìm user theo username
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Tìm user theo email
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Tìm user theo username hoặc email
     */
    Optional<User> findByUsernameOrEmail(String username, String email);
    
    /**
     * Kiểm tra username đã tồn tại chưa
     */
    boolean existsByUsername(String username);
    
    /**
     * Kiểm tra email đã tồn tại chưa
     */
    boolean existsByEmail(String email);
    
    /**
     * Tìm tất cả user theo status
     */
    List<User> findByStatus(User.Status status);
    
    /**
     * Tìm tất cả user theo role
     */
    List<User> findByRole(User.Role role);
    
    /**
     * Tìm user theo fullname chứa từ khóa (không phân biệt hoa thường)
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.fullname) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> findByFullnameContainingIgnoreCase(@Param("keyword") String keyword);
    
    // ==================== ADMIN METHODS ====================
    
    /**
     * Tìm user theo role với pagination
     */
    Page<User> findByRole(User.Role role, Pageable pageable);
    
    /**
     * Tìm user theo status với pagination
     */
    Page<User> findByStatus(User.Status status, Pageable pageable);
    
    /**
     * Tìm user theo role và status với pagination
     */
    Page<User> findByRoleAndStatus(User.Role role, User.Status status, Pageable pageable);
    
    /**
     * Tìm kiếm user theo keyword (username, email, fullname) với pagination
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.fullname) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<User> findByKeywordWithPagination(@Param("keyword") String keyword, Pageable pageable);
    
    /**
     * Đếm user theo role
     */
    long countByRole(User.Role role);
    
    /**
     * Đếm user theo status
     */
    long countByStatus(User.Status status);
    
    /**
     * Tìm user đăng ký gần đây (không bao gồm user đã xóa)
     */
    @Query("SELECT u FROM User u WHERE u.status <> :deletedStatus ORDER BY u.createdAt DESC")
    List<User> findRecentUsers(@Param("deletedStatus") User.Status deletedStatus, Pageable pageable);
}
