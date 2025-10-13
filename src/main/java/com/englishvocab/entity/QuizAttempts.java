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
@Table(name = "quiz_attempts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class QuizAttempts {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_attempt_id")
    Integer quizAttemptId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    Quizzes quiz;
    
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
    
    // Relationships
    @OneToMany(mappedBy = "quizAttempt", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<QuizItemResults> itemResults;
    
    /**
     * Convenience methods
     */
    public boolean isCompleted() {
        return submittedAt != null;
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
        if (quiz == null || quiz.getPassScore() == null) return true;
        return getPercentageScore() >= quiz.getPassScore();
    }
    
    public String getResultStatus() {
        if (!isCompleted()) return "Đang làm";
        return isPassed() ? "Đậu" : "Rớt";
    }
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
}
