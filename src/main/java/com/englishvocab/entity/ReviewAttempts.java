package com.englishvocab.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "review_attempts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class ReviewAttempts {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_attempt_id")
    Integer reviewAttemptId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    Reviews review;
    
    @Column(name = "started_at")
    LocalDateTime startedAt;
    
    @Column(name = "submitted_at")
    LocalDateTime submittedAt;
    
    @Column(name = "duration_sec")
    Integer durationSec; // Time taken in seconds
    
    @Column(nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    Integer score = 0; // Score achieved
    
    @Column(name = "max_score", nullable = false, columnDefinition = "integer default 100")
    @Builder.Default
    Integer maxScore = 100; // Maximum possible score
    
    @Column(name = "attempt_type")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    AttemptType attemptType = AttemptType.REVIEW;
    
    // Relationships
    @OneToMany(mappedBy = "reviewAttempt", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<ReviewItemResults> itemResults;
    
    public enum AttemptType {
        REVIEW,     // Ôn tập từ vựng
        QUIZ,       // Quiz thông thường
        EXAM,       // Thi cử
        PRACTICE    // Luyện tập
    }
    
    /**
     * Convenience methods
     */
    public boolean isCompleted() {
        return submittedAt != null;
    }
    
    public boolean isReviewAttempt() {
        return attemptType == AttemptType.REVIEW;
    }
    
    public double getPercentageScore() {
        if (maxScore == null || maxScore == 0) return 0.0;
        return (score * 100.0) / maxScore;
    }
    
    public String getFormattedScore() {
        return score + "/" + maxScore + " (" + String.format("%.1f", getPercentageScore()) + "%)";
    }
    
    public String getFormattedDuration() {
        if (durationSec == null) return "N/A";
        
        int minutes = durationSec / 60;
        int seconds = durationSec % 60;
        
        if (minutes > 0) {
            return minutes + ":" + String.format("%02d", seconds);
        } else {
            return "0:" + String.format("%02d", seconds);
        }
    }
    
    public boolean isPassed() {
        if (review == null || review.getPassScore() == null) return true;
        return getPercentageScore() >= review.getPassScore();
    }
    
    public String getResultStatus() {
        if (!isCompleted()) return "Đang làm";
        return isPassed() ? "Đậu" : "Rớt";
    }
    
    public String getAttemptTypeDisplayName() {
        return switch (attemptType) {
            case REVIEW -> "Ôn tập";
            case QUIZ -> "Quiz";
            case EXAM -> "Thi cử";
            case PRACTICE -> "Luyện tập";
        };
    }
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
}
