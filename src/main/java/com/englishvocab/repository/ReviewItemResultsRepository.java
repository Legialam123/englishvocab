package com.englishvocab.repository;

import com.englishvocab.entity.ReviewAttempts;
import com.englishvocab.entity.ReviewItemResults;
import com.englishvocab.entity.ReviewItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewItemResultsRepository extends JpaRepository<ReviewItemResults, Integer> {
    
    /**
     * Find results by review attempt
     */
    List<ReviewItemResults> findByReviewAttempt(ReviewAttempts reviewAttempt);
    
    /**
     * Find results by review attempt ID
     */
    List<ReviewItemResults> findByReviewAttemptReviewAttemptId(Integer reviewAttemptId);
    
    /**
     * Find results by review item
     */
    List<ReviewItemResults> findByReviewItem(ReviewItems reviewItem);
    
    /**
     * Find correct results by review attempt
     */
    @Query("SELECT rir FROM ReviewItemResults rir WHERE rir.reviewAttempt = :attempt AND rir.isCorrect = true")
    List<ReviewItemResults> findCorrectResultsByAttempt(@Param("attempt") ReviewAttempts attempt);
    
    /**
     * Find incorrect results by review attempt
     */
    @Query("SELECT rir FROM ReviewItemResults rir WHERE rir.reviewAttempt = :attempt AND rir.isCorrect = false")
    List<ReviewItemResults> findIncorrectResultsByAttempt(@Param("attempt") ReviewAttempts attempt);
    
    /**
     * Count correct answers by review attempt
     */
    @Query("SELECT COUNT(rir) FROM ReviewItemResults rir WHERE rir.reviewAttempt = :attempt AND rir.isCorrect = true")
    long countCorrectAnswersByAttempt(@Param("attempt") ReviewAttempts attempt);
    
    /**
     * Count total answers by review attempt
     */
    long countByReviewAttempt(ReviewAttempts reviewAttempt);
    
    /**
     * Find results by vocabulary (for review feature)
     */
    @Query("SELECT rir FROM ReviewItemResults rir WHERE rir.reviewItem.vocab.vocabId = :vocabId AND rir.reviewAttempt.attemptType = 'REVIEW'")
    List<ReviewItemResults> findVocabularyReviewResults(@Param("vocabId") Integer vocabId);
    
    /**
     * Find results by user and vocabulary
     */
    @Query("SELECT rir FROM ReviewItemResults rir WHERE rir.reviewAttempt.user.id = :userId AND rir.reviewItem.vocab.vocabId = :vocabId")
    List<ReviewItemResults> findByUserAndVocabulary(@Param("userId") Integer userId, @Param("vocabId") Integer vocabId);
    
    /**
     * Find recent results by user
     */
    @Query("SELECT rir FROM ReviewItemResults rir WHERE rir.reviewAttempt.user.id = :userId ORDER BY rir.answeredAt DESC")
    List<ReviewItemResults> findRecentResultsByUser(@Param("userId") Integer userId);
    
    /**
     * Calculate average score by review attempt
     */
    @Query("SELECT AVG(rir.score) FROM ReviewItemResults rir WHERE rir.reviewAttempt = :attempt")
    Double calculateAverageScoreByAttempt(@Param("attempt") ReviewAttempts attempt);
}
