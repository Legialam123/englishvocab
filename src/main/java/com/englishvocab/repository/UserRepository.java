package com.englishvocab.repository;

import com.englishvocab.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
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
}
