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
import java.util.List;

@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class Reviews {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    Integer reviewId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dictionary_id")
    Dictionary dictionary; // Nullable for general review
    
    @Column(nullable = false, length = 100)
    @NotBlank(message = "Tiêu đề review không được để trống")
    @Size(max = 100, message = "Tiêu đề không được vượt quá 100 ký tự")
    String title;
    
    @Column(length = 10)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    Status status = Status.ACTIVE;
    
    @Column(name = "time_limit_sec")
    Integer timeLimitSec; // Time limit in seconds
    
    @Column(name = "num_items")
    Integer numItems; // Number of questions
    
    @Column(name = "pass_score")
    Integer passScore; // Minimum score to pass (percentage)
    
    @Column(name = "review_type")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    ReviewType reviewType = ReviewType.VOCABULARY_REVIEW;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
    
    // Relationships
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<ReviewItems> reviewItems;
    
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<ReviewAttempts> reviewAttempts;
    
    public enum Status {
        ACTIVE, INACTIVE, DRAFT
    }
    
    public enum ReviewType {
        VOCABULARY_REVIEW,  // Ôn tập từ vựng
        QUIZ,               // Quiz thông thường
        EXAM,               // Thi cử
        PRACTICE            // Luyện tập
    }
    
    /**
     * Convenience methods
     */
    public boolean isActive() {
        return status == Status.ACTIVE;
    }
    
    public boolean isVocabularyReview() {
        return reviewType == ReviewType.VOCABULARY_REVIEW;
    }
    
    public String getFormattedTimeLimit() {
        if (timeLimitSec == null) return "Không giới hạn";
        
        int minutes = timeLimitSec / 60;
        int seconds = timeLimitSec % 60;
        
        if (minutes > 0) {
            return minutes + " phút" + (seconds > 0 ? " " + seconds + " giây" : "");
        } else {
            return seconds + " giây";
        }
    }
    
    public String getDisplayInfo() {
        return title + " (" + (numItems != null ? numItems : 0) + " câu hỏi)";
    }
    
    public String getReviewTypeDisplayName() {
        return switch (reviewType) {
            case VOCABULARY_REVIEW -> "Ôn tập từ vựng";
            case QUIZ -> "Quiz";
            case EXAM -> "Thi cử";
            case PRACTICE -> "Luyện tập";
        };
    }
}
