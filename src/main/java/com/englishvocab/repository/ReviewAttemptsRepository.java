package com.englishvocab.repository;

import com.englishvocab.entity.ReviewAttempts;
import com.englishvocab.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewAttemptsRepository extends JpaRepository<ReviewAttempts, Integer> {
    
    /**
     * Find review attempts by user
     */
    List<ReviewAttempts> findByUser(User user);
    
    /**
     * Find review attempts by user ordered by start time
     */
    @Query("SELECT ra FROM ReviewAttempts ra WHERE ra.user = :user ORDER BY ra.startedAt DESC")
    List<ReviewAttempts> findByUserOrderByStartedAtDesc(@Param("user") User user);
    
    /**
     * Find review attempts by review
     */
    List<ReviewAttempts> findByReviewReviewId(Integer reviewId);
    
    /**
     * Find review attempts by user and review
     */
    List<ReviewAttempts> findByUserAndReviewReviewId(User user, Integer reviewId);
    
    /**
     * Find completed review attempts
     */
    @Query("SELECT ra FROM ReviewAttempts ra WHERE ra.submittedAt IS NOT NULL")
    List<ReviewAttempts> findCompletedAttempts();
    
    /**
     * Find vocabulary review attempts (for review feature)
     */
    @Query("SELECT ra FROM ReviewAttempts ra WHERE ra.attemptType = 'REVIEW'")
    List<ReviewAttempts> findVocabularyReviewAttempts();
    
    /**
     * Find review attempts by user and attempt type
     */
    List<ReviewAttempts> findByUserAndAttemptType(User user, ReviewAttempts.AttemptType attemptType);
    
    /**
     * Find recent review attempts by user
     */
    @Query("SELECT ra FROM ReviewAttempts ra WHERE ra.user = :user AND ra.startedAt >= :since ORDER BY ra.startedAt DESC")
    List<ReviewAttempts> findRecentAttemptsByUser(@Param("user") User user, @Param("since") LocalDateTime since);
    
    /**
     * Count review attempts by user
     */
    long countByUser(User user);
    
    /**
     * Count review attempts by user and attempt type
     */
    long countByUserAndAttemptType(User user, ReviewAttempts.AttemptType attemptType);
    
    /**
     * Find best score for a review
     */
    @Query("SELECT MAX(ra.score) FROM ReviewAttempts ra WHERE ra.review.reviewId = :reviewId AND ra.user = :user")
    Integer findBestScoreByReviewAndUser(@Param("reviewId") Integer reviewId, @Param("user") User user);
    
    /**
     * Find top review attempt by user ordered by start time
     */
    @Query("SELECT ra FROM ReviewAttempts ra WHERE ra.user = :user ORDER BY ra.startedAt DESC LIMIT 1")
    Optional<ReviewAttempts> findTopByUserOrderByStartedAtDesc(@Param("user") User user);
}
