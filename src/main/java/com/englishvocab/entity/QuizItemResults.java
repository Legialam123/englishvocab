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
@Table(name = "quiz_item_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class QuizItemResults {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_item_result_id")
    Integer quizItemResultId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_item_id", nullable = false)
    QuizItems quizItem;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_attempt_id", nullable = false)
    QuizAttempts quizAttempt;
    
    @Column(name = "is_correct", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    Boolean isCorrect = false;
    
    @Column(nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    Integer score = 0;
    
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
}
