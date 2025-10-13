package com.englishvocab.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    Integer notificationId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;
    
    @Column(nullable = false, length = 100)
    @NotBlank(message = "Tiêu đề thông báo không được để trống")
    @Size(max = 100, message = "Tiêu đề không được vượt quá 100 ký tự")
    String title;
    
    @Column(nullable = false, length = 200)
    @NotBlank(message = "Nội dung thông báo không được để trống")
    @Size(max = 200, message = "Nội dung không được vượt quá 200 ký tự")
    String message;
    
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    Type type = Type.INFO;
    
    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    Status status = Status.UNREAD;
    
    @CreationTimestamp
    @Column(name = "sent_at", updatable = false)
    LocalDateTime sentAt;
    
    public enum Type {
        INFO,           // Thông tin
        REMINDER,       // Nhắc nhở
        ACHIEVEMENT,    // Thành tựu
        QUIZ_RESULT,    // Kết quả quiz
        SYSTEM         // Hệ thống
    }
    
    public enum Status {
        READ, UNREAD
    }
    
    /**
     * Convenience methods
     */
    public boolean isRead() {
        return status == Status.READ;
    }
    
    public void markAsRead() {
        this.status = Status.READ;
    }
    
    public String getTypeDisplayName() {
        return switch (type) {
            case INFO -> "Thông tin";
            case REMINDER -> "Nhắc nhở";
            case ACHIEVEMENT -> "Thành tựu";
            case QUIZ_RESULT -> "Kết quả quiz";
            case SYSTEM -> "Hệ thống";
        };
    }
    
    public String getTypeIcon() {
        return switch (type) {
            case INFO -> "ℹ️";
            case REMINDER -> "⏰";
            case ACHIEVEMENT -> "🏆";
            case QUIZ_RESULT -> "📊";
            case SYSTEM -> "⚙️";
        };
    }
    
    public String getFormattedTime() {
        if (sentAt == null) return "N/A";
        
        LocalDateTime now = LocalDateTime.now();
        if (sentAt.isAfter(now.minusHours(1))) {
            return "Vừa xong";
        } else if (sentAt.isAfter(now.minusDays(1))) {
            long hours = java.time.Duration.between(sentAt, now).toHours();
            return hours + " giờ trước";
        } else {
            long days = java.time.Duration.between(sentAt, now).toDays();
            return days + " ngày trước";
        }
    }
}
