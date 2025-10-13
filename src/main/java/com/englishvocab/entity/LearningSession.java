package com.englishvocab.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity quản lý phiên học (Learning Session)
 * Mỗi session là một lượt học từ vựng với danh sách từ cụ thể
 */
@Entity
@Table(name = "learning_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LearningSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    Long sessionId;
    
    @Column(name = "session_uuid", unique = true, nullable = false, length = 36)
    @Builder.Default
    String sessionUuid = UUID.randomUUID().toString();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"password", "googleId", "hibernateLazyInitializer", "handler"})
    User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dictionary_id", nullable = false)
    @JsonIgnoreProperties({"vocabularies", "userDictLists"})
    Dictionary dictionary;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "learning_mode", nullable = false, length = 20)
    LearningMode learningMode;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    Status status = Status.ACTIVE;
    
    @Column(name = "target_words", nullable = false)
    Integer targetWords; // Số từ dự định học
    
    @Column(name = "actual_words")
    @Builder.Default
    Integer actualWords = 0; // Số từ thực tế đã học
    
    @Column(name = "correct_count")
    @Builder.Default
    Integer correctCount = 0;
    
    @Column(name = "wrong_count")
    @Builder.Default
    Integer wrongCount = 0;
    
    @Column(name = "skip_count")
    @Builder.Default
    Integer skipCount = 0;
    
    @Column(name = "time_spent_sec")
    @Builder.Default
    Integer timeSpentSec = 0; // Tổng thời gian (giây)
    
    @Column(name = "started_at")
    LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    LocalDateTime completedAt;
    
    @Column(name = "last_activity_at")
    LocalDateTime lastActivityAt;
    
    @Column(name = "expires_at")
    LocalDateTime expiresAt; // Session timeout
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
    
    // Relationships
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    List<SessionVocabulary> sessionVocabularies = new ArrayList<>();
    
    /**
     * Enum cho Learning Mode
     */
    public enum LearningMode {
        ALPHABETICAL,  // Học theo A-Z
        TOPICS,        // Học theo chủ đề
        CUSTOM,        // Tự chọn từ
        REVIEW         // Ôn tập từ cần review
    }
    
    /**
     * Enum cho Session Status
     */
    public enum Status {
        ACTIVE,     // Đang học
        PAUSED,     // Tạm dừng
        COMPLETED,  // Hoàn thành
        EXPIRED,    // Hết hạn (timeout)
        CANCELLED   // Bị hủy
    }
    
    /**
     * Helper methods
     */
    
    public boolean isActive() {
        return status == Status.ACTIVE;
    }
    
    public boolean isPaused() {
        return status == Status.PAUSED;
    }
    
    public boolean isCompleted() {
        return status == Status.COMPLETED;
    }
    
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean canResume() {
        return (status == Status.PAUSED || status == Status.ACTIVE) && !isExpired();
    }
    
    public double getAccuracyPercentage() {
        int total = correctCount + wrongCount;
        if (total == 0) return 0.0;
        return (correctCount * 100.0) / total;
    }
    
    public int getProgress() {
        if (targetWords == 0) return 0;
        return (actualWords * 100) / targetWords;
    }
    
    public String getFormattedDuration() {
        if (timeSpentSec == null || timeSpentSec == 0) return "0:00";
        
        int minutes = timeSpentSec / 60;
        int seconds = timeSpentSec % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
    
    public void incrementCorrect() {
        this.correctCount++;
        this.actualWords++;
        updateActivity();
    }
    
    public void incrementWrong() {
        this.wrongCount++;
        this.actualWords++;
        updateActivity();
    }
    
    public void incrementSkip() {
        this.skipCount++;
        this.actualWords++;
        updateActivity();
    }
    
    public void updateActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }
    
    public void pause() {
        this.status = Status.PAUSED;
        updateActivity();
    }
    
    public void resume() {
        if (canResume()) {
            this.status = Status.ACTIVE;
            // Extend expiration by 30 minutes from now
            this.expiresAt = LocalDateTime.now().plusMinutes(30);
            updateActivity();
        }
    }
    
    public void complete() {
        this.status = Status.COMPLETED;
        this.completedAt = LocalDateTime.now();
        updateActivity();
    }
    
    public void expire() {
        this.status = Status.EXPIRED;
        updateActivity();
    }
    
    public void cancel() {
        this.status = Status.CANCELLED;
        updateActivity();
    }
}
