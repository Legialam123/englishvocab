package com.englishvocab.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_item_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class ReviewItemResults {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_item_result_id")
    Integer reviewItemResultId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_item_id", nullable = false)
    ReviewItems reviewItem;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_attempt_id", nullable = false)
    ReviewAttempts reviewAttempt;
    
    @Column(name = "is_correct", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    Boolean isCorrect = false;
    
    @Column(nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    Integer score = 0;
    
    @Column(name = "user_answer", length = 200)
    String userAnswer; // User's answer
    
    @Column(name = "time_taken_sec")
    Integer timeTakenSec; // Time taken to answer this question
    
    @CreationTimestamp
    @Column(name = "answered_at", updatable = false)
    LocalDateTime answeredAt;
    
    /**
     * Convenience methods
     */
    public String getResultDisplayName() {
        return isCorrect ? "Đúng" : "Sai";
    }
    
    public String getResultIcon() {
        return isCorrect ? "✅" : "❌";
    }
    
    public String getResultCssClass() {
        return isCorrect ? "text-success" : "text-danger";
    }
    
    public String getFormattedTimeTaken() {
        if (timeTakenSec == null) return "N/A";
        
        int minutes = timeTakenSec / 60;
        int seconds = timeTakenSec % 60;
        
        if (minutes > 0) {
            return minutes + ":" + String.format("%02d", seconds);
        } else {
            return "0:" + String.format("%02d", seconds);
        }
    }
    
    public boolean isVocabularyReview() {
        return reviewAttempt != null && reviewAttempt.isReviewAttempt();
    }
}
