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
@Table(name = "quizzes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class Quizzes {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_id")
    Integer quizId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dictionary_id", nullable = false)
    Dictionary dictionary;
    
    @Column(nullable = false, length = 100)
    @NotBlank(message = "Tiêu đề quiz không được để trống")
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
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
    
    // Relationships
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<QuizItems> quizItems;
    
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<QuizAttempts> quizAttempts;
    
    public enum Status {
        ACTIVE, INACTIVE, DRAFT
    }
    
    /**
     * Convenience methods
     */
    public boolean isActive() {
        return status == Status.ACTIVE;
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
}
