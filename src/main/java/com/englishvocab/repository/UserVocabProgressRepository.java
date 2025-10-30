package com.englishvocab.repository;

import com.englishvocab.entity.Dictionary;
import com.englishvocab.entity.User;
import com.englishvocab.entity.UserVocabProgress;
import com.englishvocab.entity.Vocab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserVocabProgressRepository extends JpaRepository<UserVocabProgress, Integer> {
    
    /**
     * Tìm progress của user với vocab cụ thể
     */
    Optional<UserVocabProgress> findByUserAndVocab(User user, Vocab vocab);
    
    /**
     * Tìm tất cả progress của user
     */
    List<UserVocabProgress> findByUser(User user);
    
    /**
     * Tìm progress của user theo status
     */
    List<UserVocabProgress> findByUserAndStatus(User user, UserVocabProgress.Status status);
    
    /**
     * Tìm words cần review hôm nay (SRS)
     */
    @Query("SELECT uvp FROM UserVocabProgress uvp WHERE uvp.user = :user AND uvp.nextReviewAt <= :now ORDER BY uvp.nextReviewAt")
    List<UserVocabProgress> findDueForReview(@Param("user") User user, @Param("now") LocalDateTime now);
    
    /**
     * Tìm words theo box (Leitner)
     */
    List<UserVocabProgress> findByUserAndBox(User user, Integer box);
    
    /**
     * Đếm từ đã học của user
     */
    long countByUser(User user);
    
    /**
     * Đếm từ theo status của user
     */
    long countByUserAndStatus(User user, UserVocabProgress.Status status);
    
    /**
     * Đếm từ cần review
     */
    @Query("SELECT COUNT(uvp) FROM UserVocabProgress uvp WHERE uvp.user = :user AND uvp.nextReviewAt <= :now")
    long countDueForReview(@Param("user") User user, @Param("now") LocalDateTime now);

    /**
     * Đếm từ đã học của user trong 1 cuốn từ điển
     */
    long countByUserAndVocab_Dictionary(User user, Dictionary dictionary);

    /**
     * Đếm từ cần review trong 1 cuốn từ điển
     */
    @Query("SELECT COUNT(uvp) FROM UserVocabProgress uvp WHERE uvp.user = :user AND uvp.nextReviewAt <= :now AND uvp.vocab.dictionary = :dictionary")
    long countDueForReviewInDictionary(@Param("user") User user, @Param("now") LocalDateTime now, @Param("dictionary") Dictionary dictionary);

    /**
     * Tìm từ khó nhớ (wrong count cao)
     */
    @Query("SELECT uvp FROM UserVocabProgress uvp WHERE uvp.user = :user AND uvp.wrongCount >= :minWrongCount ORDER BY uvp.wrongCount DESC")
    List<UserVocabProgress> findDifficultWords(@Param("user") User user, @Param("minWrongCount") int minWrongCount);
    
    /**
     * Tìm từ đã mastered
     */
    @Query("SELECT uvp FROM UserVocabProgress uvp WHERE uvp.user = :user AND uvp.status = 'MASTERED' ORDER BY uvp.lastReviewed DESC")
    List<UserVocabProgress> findMasteredWords(@Param("user") User user);
    
    /**
     * Thống kê theo box
     */
    @Query("SELECT uvp.box as box, COUNT(uvp) as count FROM UserVocabProgress uvp WHERE uvp.user = :user GROUP BY uvp.box ORDER BY uvp.box")
    List<Object[]> getBoxStatistics(@Param("user") User user);
    
    /**
     * Tìm progress theo dictionary
     */
    @Query("SELECT uvp FROM UserVocabProgress uvp WHERE uvp.user = :user AND uvp.vocab.dictionary.dictionaryId = :dictionaryId")
    List<UserVocabProgress> findByUserAndDictionary(@Param("user") User user, @Param("dictionaryId") Integer dictionaryId);
    
    /**
     * Daily study streak calculation (with eager fetch)
     */
    @Query("SELECT uvp FROM UserVocabProgress uvp " +
           "LEFT JOIN FETCH uvp.vocab v " +
           "WHERE uvp.user = :user AND uvp.lastReviewed >= :startDate " +
           "ORDER BY uvp.lastReviewed DESC")
    List<UserVocabProgress> findRecentActivity(@Param("user") User user, @Param("startDate") LocalDateTime startDate);
}
