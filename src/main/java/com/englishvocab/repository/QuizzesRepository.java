package com.englishvocab.repository;

import com.englishvocab.entity.Dictionary;
import com.englishvocab.entity.Quizzes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizzesRepository extends JpaRepository<Quizzes, Integer> {
    
    /**
     * Tìm quiz theo dictionary
     */
    List<Quizzes> findByDictionary(Dictionary dictionary);
    
    /**
     * Tìm quiz active theo dictionary
     */
    List<Quizzes> findByDictionaryAndStatus(Dictionary dictionary, Quizzes.Status status);
    
    /**
     * Tìm tất cả quiz active
     */
    List<Quizzes> findByStatusOrderByTitle(Quizzes.Status status);
    
    /**
     * Tìm quiz theo title
     */
    @Query("SELECT q FROM Quizzes q WHERE LOWER(q.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY q.title")
    List<Quizzes> findByTitleContainingIgnoreCase(@Param("keyword") String keyword);
    
    /**
     * Tìm quiz với phân trang
     */
    Page<Quizzes> findByStatusOrderByCreatedAtDesc(Quizzes.Status status, Pageable pageable);
    
    /**
     * Đếm quiz theo status
     */
    long countByStatus(Quizzes.Status status);
    
    /**
     * Đếm quiz theo dictionary
     */
    long countByDictionary(Dictionary dictionary);
    
    /**
     * Tìm quiz theo số lượng câu hỏi
     */
    List<Quizzes> findByNumItemsBetween(Integer minItems, Integer maxItems);
    
    /**
     * Tìm quiz theo thời gian (có time limit)
     */
    @Query("SELECT q FROM Quizzes q WHERE q.timeLimitSec IS NOT NULL AND q.status = 'ACTIVE' ORDER BY q.timeLimitSec")
    List<Quizzes> findQuizzesWithTimeLimit();
    
    /**
     * Tìm quiz theo độ khó (pass score)
     */
    @Query("SELECT q FROM Quizzes q WHERE q.passScore >= :minPassScore AND q.status = 'ACTIVE' ORDER BY q.passScore DESC")
    List<Quizzes> findQuizzesByDifficulty(@Param("minPassScore") Integer minPassScore);
}
