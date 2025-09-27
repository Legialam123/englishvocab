package com.englishvocab.repository;

import com.englishvocab.entity.User;
import com.englishvocab.entity.UserCustomVocab;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserCustomVocabRepository extends JpaRepository<UserCustomVocab, Integer> {
    
    /**
     * Tìm từ vựng cá nhân của user
     */
    List<UserCustomVocab> findByUserOrderByCreatedAtDesc(User user);
    
    /**
     * Tìm từ vựng cá nhân của user với phân trang
     */
    Page<UserCustomVocab> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    /**
     * Kiểm tra từ vựng đã tồn tại cho user
     */
    boolean existsByUserAndNameIgnoreCase(User user, String name);
    
    /**
     * Tìm từ vựng theo tên (search)
     */
    @Query("SELECT ucv FROM UserCustomVocab ucv WHERE ucv.user = :user AND LOWER(ucv.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<UserCustomVocab> findByUserAndNameContainingIgnoreCase(@Param("user") User user, @Param("keyword") String keyword);
    
    /**
     * Tìm kiếm với multiple fields
     */
    @Query("SELECT ucv FROM UserCustomVocab ucv WHERE ucv.user = :user AND " +
           "(LOWER(ucv.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(ucv.meaningVi) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(ucv.pos) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<UserCustomVocab> findByUserAndSearchTerm(@Param("user") User user, @Param("searchTerm") String searchTerm);
    
    /**
     * Đếm từ vựng của user
     */
    long countByUser(User user);
    
    /**
     * Đếm từ vựng theo từ loại
     */
    long countByUserAndPos(User user, String pos);
    
    /**
     * Tìm từ vựng theo từ loại
     */
    List<UserCustomVocab> findByUserAndPosOrderByCreatedAtDesc(User user, String pos);
    
    /**
     * Tìm từ vựng mới nhất
     */
    @Query("SELECT ucv FROM UserCustomVocab ucv WHERE ucv.user = :user ORDER BY ucv.createdAt DESC")
    Page<UserCustomVocab> findRecentByUser(@Param("user") User user, Pageable pageable);
    
    /**
     * Tìm từ vựng theo tháng tạo
     */
    @Query("SELECT ucv FROM UserCustomVocab ucv WHERE ucv.user = :user AND " +
           "YEAR(ucv.createdAt) = :year AND MONTH(ucv.createdAt) = :month " +
           "ORDER BY ucv.createdAt DESC")
    List<UserCustomVocab> findByUserAndMonth(@Param("user") User user, @Param("year") int year, @Param("month") int month);
    
    /**
     * Thống kê theo từ loại
     */
    @Query("SELECT ucv.pos, COUNT(ucv) FROM UserCustomVocab ucv WHERE ucv.user = :user GROUP BY ucv.pos")
    List<Object[]> getPartOfSpeechStatistics(@Param("user") User user);
    
    /**
     * Tìm từ vựng duplicates (để cleanup)
     */
    @Query("SELECT ucv FROM UserCustomVocab ucv WHERE ucv.user = :user AND " +
           "EXISTS (SELECT ucv2 FROM UserCustomVocab ucv2 WHERE ucv2.user = :user AND " +
           "ucv2.name = ucv.name AND ucv2.customVocabId < ucv.customVocabId)")
    List<UserCustomVocab> findDuplicates(@Param("user") User user);
}
