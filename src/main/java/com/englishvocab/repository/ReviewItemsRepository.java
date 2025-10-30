package com.englishvocab.repository;

import com.englishvocab.entity.ReviewItems;
import com.englishvocab.entity.Reviews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewItemsRepository extends JpaRepository<ReviewItems, Integer> {
    
    /**
     * Find review items by review
     */
    List<ReviewItems> findByReview(Reviews review);
    
    /**
     * Find review items by review ID
     */
    List<ReviewItems> findByReviewReviewId(Integer reviewId);
    
    /**
     * Find review items by review ordered by ID
     */
    @Query("SELECT ri FROM ReviewItems ri WHERE ri.review.reviewId = :reviewId ORDER BY ri.reviewItemId")
    List<ReviewItems> findByReviewOrderByReviewItemId(@Param("reviewId") Integer reviewId);
    
    /**
     * Find review items by review ordered by session order
     */
    @Query("SELECT ri FROM ReviewItems ri WHERE ri.review.reviewId = :reviewId ORDER BY ri.sessionOrder ASC")
    List<ReviewItems> findByReviewOrderByCreatedAtAsc(@Param("reviewId") Integer reviewId);
    
    /**
     * Find review items by type
     */
    List<ReviewItems> findByType(ReviewItems.Type type);
    
    /**
     * Find review items by difficulty
     */
    List<ReviewItems> findByDifficulty(ReviewItems.Difficulty difficulty);
    
    /**
     * Find review items by vocabulary
     */
    List<ReviewItems> findByVocabVocabId(Integer vocabId);
    
    /**
     * Find review items by custom vocabulary
     */
    List<ReviewItems> findByCustomVocabCustomVocabId(Integer customVocabId);
    
    /**
     * Count review items by review
     */
    long countByReview(Reviews review);
    
    /**
     * Find review items by review and type
     */
    List<ReviewItems> findByReviewAndType(Reviews review, ReviewItems.Type type);
    
    /**
     * Find review item by review and session order
     */
    @Query("SELECT ri FROM ReviewItems ri WHERE ri.review = :review AND ri.sessionOrder = :sessionOrder")
    java.util.Optional<ReviewItems> findByReviewAndSessionOrder(@Param("review") Reviews review, @Param("sessionOrder") Integer sessionOrder);
    
    /**
     * Find vocabulary review items (for review feature)
     */
    @Query("SELECT ri FROM ReviewItems ri WHERE ri.review.reviewType = 'VOCABULARY_REVIEW'")
    List<ReviewItems> findVocabularyReviewItems();
}
