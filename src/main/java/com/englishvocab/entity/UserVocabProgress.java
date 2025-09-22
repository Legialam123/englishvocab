package com.englishvocab.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_vocab_progress")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVocabProgress {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vocab_progress_id")
    private Integer vocabProgressId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocab_id", nullable = false)
    private Vocab vocab;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "custom_vocab_id")
    private Integer customVocabId; // For custom vocabulary
    
    @Column(name = "first_learned")
    private LocalDateTime firstLearned;
    
    @Column(name = "last_reviewed")
    private LocalDateTime lastReviewed;
    
    @Column(name = "next_review_at")
    private LocalDateTime nextReviewAt;
    
    @Column(nullable = false, columnDefinition = "integer default 1")
    @Builder.Default
    private Integer box = 1; // Leitner Box (1-5)
    
    @Column(nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private Integer streak = 0; // Consecutive correct answers
    
    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.LEARNING;
    
    @Column(name = "wrong_count", nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private Integer wrongCount = 0;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum Status {
        NEW,        // Chưa học
        LEARNING,   // Đang học
        REVIEWING,  // Đang ôn tập  
        MASTERED,   // Đã thành thạo
        DIFFICULT   // Khó nhớ
    }
    
    /**
     * Simple Leitner Algorithm Methods
     */
    public void markCorrect() {
        this.streak++;
        this.lastReviewed = LocalDateTime.now();
        
        // Move to higher box (max 5)
        if (this.box < 5) {
            this.box++;
        }
        
        // Calculate next review date based on box
        this.nextReviewAt = calculateNextReviewDate();
        
        // Update status
        if (this.box >= 4) {
            this.status = Status.MASTERED;
        } else {
            this.status = Status.REVIEWING;
        }
    }
    
    public void markIncorrect() {
        this.streak = 0;
        this.wrongCount++;
        this.lastReviewed = LocalDateTime.now();
        
        // Move back to box 1 (daily review)
        this.box = 1;
        this.nextReviewAt = LocalDateTime.now().plusDays(1);
        
        // Update status
        if (this.wrongCount >= 3) {
            this.status = Status.DIFFICULT;
        } else {
            this.status = Status.LEARNING;
        }
    }
    
    public LocalDateTime calculateNextReviewDate() {
        LocalDateTime now = LocalDateTime.now();
        return switch (this.box) {
            case 1 -> now.plusDays(1);      // Box 1: Daily
            case 2 -> now.plusDays(3);      // Box 2: Every 3 days
            case 3 -> now.plusDays(7);      // Box 3: Weekly
            case 4 -> now.plusDays(14);     // Box 4: Bi-weekly
            case 5 -> now.plusDays(30);     // Box 5: Monthly
            default -> now.plusDays(1);
        };
    }
    
    public boolean isDueForReview() {
        return nextReviewAt != null && LocalDateTime.now().isAfter(nextReviewAt);
    }
    
    public String getBoxDisplayName() {
        return switch (this.box) {
            case 1 -> "Hàng ngày";
            case 2 -> "3 ngày";
            case 3 -> "Hàng tuần";
            case 4 -> "2 tuần";
            case 5 -> "Hàng tháng";
            default -> "Không xác định";
        };
    }
}
