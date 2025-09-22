package com.englishvocab.repository;

import com.englishvocab.entity.Notification;
import com.englishvocab.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    
    /**
     * Tìm thông báo theo user
     */
    List<Notification> findByUserOrderBySentAtDesc(User user);
    
    /**
     * Tìm thông báo chưa đọc của user
     */
    List<Notification> findByUserAndStatusOrderBySentAtDesc(User user, Notification.Status status);
    
    /**
     * Tìm thông báo với phân trang
     */
    Page<Notification> findByUserOrderBySentAtDesc(User user, Pageable pageable);
    
    /**
     * Tìm thông báo theo type
     */
    List<Notification> findByUserAndTypeOrderBySentAtDesc(User user, Notification.Type type);
    
    /**
     * Đếm thông báo chưa đọc
     */
    long countByUserAndStatus(User user, Notification.Status status);
    
    /**
     * Đếm tổng thông báo của user
     */
    long countByUser(User user);
    
    /**
     * Tìm thông báo trong khoảng thời gian
     */
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.sentAt BETWEEN :startDate AND :endDate ORDER BY n.sentAt DESC")
    List<Notification> findByUserAndDateRange(@Param("user") User user, 
                                            @Param("startDate") LocalDateTime startDate, 
                                            @Param("endDate") LocalDateTime endDate);
    
    /**
     * Tìm thông báo gần đây (7 ngày)
     */
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.sentAt >= :since ORDER BY n.sentAt DESC")
    List<Notification> findRecentNotifications(@Param("user") User user, @Param("since") LocalDateTime since);
    
    /**
     * Đánh dấu tất cả thông báo là đã đọc
     */
    @Query("UPDATE Notification n SET n.status = 'READ' WHERE n.user = :user AND n.status = 'UNREAD'")
    void markAllAsReadByUser(@Param("user") User user);
    
    /**
     * Xóa thông báo cũ (hơn 30 ngày)
     */
    @Query("DELETE FROM Notification n WHERE n.sentAt < :cutoffDate")
    void deleteOldNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);
}
