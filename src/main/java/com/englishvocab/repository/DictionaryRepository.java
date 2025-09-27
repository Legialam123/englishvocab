package com.englishvocab.repository;

import com.englishvocab.entity.Dictionary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DictionaryRepository extends JpaRepository<Dictionary, Integer> {
    
    /**
     * Tìm từ điển theo tên
     */
    Optional<Dictionary> findByName(String name);
    
    /**
     * Tìm từ điển theo code
     */
    Optional<Dictionary> findByCode(String code);
    
    /**
     * Tìm tất cả từ điển đang hoạt động
     */
    List<Dictionary> findByStatusOrderByName(Dictionary.Status status);
    
    /**
     * Tìm từ điển active
     */
    @Query("SELECT d FROM Dictionary d WHERE d.status = 'ACTIVE' ORDER BY d.name")
    List<Dictionary> findActiveDictionaries();
    
    /**
     * Tìm từ điển theo tên (không phân biệt hoa thường)
     */
    @Query("SELECT d FROM Dictionary d WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY d.name")
    List<Dictionary> findByNameContainingIgnoreCase(@Param("keyword") String keyword);
    
    /**
     * Tìm từ điển với phân trang
     */
    Page<Dictionary> findByStatusOrderByName(Dictionary.Status status, Pageable pageable);
    
    /**
     * Đếm từ điển theo status
     */
    long countByStatus(Dictionary.Status status);
    
    /**
     * Kiểm tra tồn tại theo code
     */
    boolean existsByCode(String code);
    
    /**
     * Kiểm tra tồn tại theo name
     */
    boolean existsByName(String name);
    
    // Find dictionaries that have vocabulary
    @Query("SELECT DISTINCT d FROM Dictionary d WHERE EXISTS (SELECT 1 FROM Vocab v WHERE v.dictionary = d)")
    List<Dictionary> findDictionariesWithVocabulary();
}