package com.englishvocab.repository;

import com.englishvocab.entity.LearningSession;
import com.englishvocab.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LearningSessionRepository extends JpaRepository<LearningSession, Long> {
    
    /**
     * Tìm session theo UUID
     */
    Optional<LearningSession> findBySessionUuid(String sessionUuid);
    
    /**
     * Tìm session theo UUID và user (security check)
     */
    Optional<LearningSession> findBySessionUuidAndUser(String sessionUuid, User user);
    
    /**
     * Tìm tất cả session của user
     */
    List<LearningSession> findByUserOrderByCreatedAtDesc(User user);
    
    /**
     * Tìm session đang active của user
     */
    List<LearningSession> findByUserAndStatus(User user, LearningSession.Status status);
    
    /**
     * Tìm session active hoặc paused của user
     */
    @Query("SELECT s FROM LearningSession s WHERE s.user = :user AND s.status IN ('ACTIVE', 'PAUSED') ORDER BY s.lastActivityAt DESC")
    List<LearningSession> findActiveOrPausedSessions(@Param("user") User user);
    
    /**
     * Tìm session đã hoàn thành của user
     */
    List<LearningSession> findByUserAndStatusOrderByCompletedAtDesc(User user, LearningSession.Status status);
    
    /**
     * Đếm session theo user và status
     */
    long countByUserAndStatus(User user, LearningSession.Status status);
    
    /**
     * Tìm session expired (cần cleanup)
     */
    @Query("SELECT s FROM LearningSession s WHERE s.status IN ('ACTIVE', 'PAUSED') AND s.expiresAt < :now")
    List<LearningSession> findExpiredSessions(@Param("now") LocalDateTime now);
    
    /**
     * Thống kê session của user
     */
    @Query("SELECT COUNT(s), SUM(s.correctCount), SUM(s.wrongCount), SUM(s.timeSpentSec) " +
           "FROM LearningSession s WHERE s.user = :user AND s.status = 'COMPLETED'")
    Object[] getSessionStatistics(@Param("user") User user);
    
    /**
     * Lấy session gần đây của user
     */
    List<LearningSession> findTop10ByUserOrderByCreatedAtDesc(User user);
    
    /**
     * Kiểm tra user có session active không
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM LearningSession s WHERE s.user = :user AND s.status = 'ACTIVE'")
    boolean hasActiveSession(@Param("user") User user);
}
