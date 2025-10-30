package com.englishvocab.repository;

import com.englishvocab.entity.Reviews;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Reviews, Integer> {
    
    /**
     * Find reviews by status
     */
    List<Reviews> findByStatus(Reviews.Status status);
    
    /**
     * Find reviews by type
     */
    List<Reviews> findByReviewType(Reviews.ReviewType reviewType);
    
    /**
     * Find vocabulary reviews (for review feature)
     */
    @Query("SELECT r FROM Reviews r WHERE r.reviewType = 'VOCABULARY_REVIEW' AND r.status = 'ACTIVE'")
    List<Reviews> findVocabularyReviews();
    
    /**
     * Find reviews by dictionary
     */
    List<Reviews> findByDictionaryDictionaryId(Integer dictionaryId);
    
    /**
     * Find recent reviews
     */
    @Query("SELECT r FROM Reviews r WHERE r.createdAt >= :since ORDER BY r.createdAt DESC")
    List<Reviews> findRecentReviews(@Param("since") LocalDateTime since);
    
    /**
     * Find reviews with pagination
     */
    Page<Reviews> findByStatus(Reviews.Status status, Pageable pageable);
    
    /**
     * Count reviews by type
     */
    long countByReviewType(Reviews.ReviewType reviewType);
    
    /**
     * Find reviews by title containing
     */
    List<Reviews> findByTitleContainingIgnoreCase(String title);
}
