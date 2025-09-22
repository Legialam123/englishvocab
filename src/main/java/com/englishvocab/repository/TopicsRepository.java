package com.englishvocab.repository;

import com.englishvocab.entity.Topics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicsRepository extends JpaRepository<Topics, Integer> {
    
    /**
     * Tìm topic theo tên
     */
    Optional<Topics> findByName(String name);
    
    /**
     * Tìm topic theo tên (không phân biệt hoa thường)
     */
    @Query("SELECT t FROM Topics t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY t.name")
    List<Topics> findByNameContainingIgnoreCase(@Param("keyword") String keyword);
    
    /**
     * Tìm tất cả topics sắp xếp theo tên
     */
    List<Topics> findAllByOrderByName();
    
    /**
     * Kiểm tra tồn tại theo tên
     */
    boolean existsByName(String name);
    
    /**
     * Tìm topics có từ vựng
     */
    @Query("SELECT DISTINCT t FROM Topics t JOIN t.vocabTopics vt ORDER BY t.name")
    List<Topics> findTopicsWithVocabulary();
    
    /**
     * Đếm số từ vựng theo topic
     */
    @Query("SELECT COUNT(vt) FROM VocabTopics vt WHERE vt.topic = :topic")
    long countVocabularyByTopic(@Param("topic") Topics topic);
    
    /**
     * Tìm top topics (có nhiều từ vựng nhất)
     */
    @Query("SELECT t FROM Topics t JOIN t.vocabTopics vt GROUP BY t ORDER BY COUNT(vt) DESC")
    List<Topics> findTopTopics();
}
