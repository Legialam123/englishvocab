package com.englishvocab.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "quizzes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quizzes {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_id")
    private Integer quizId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dictionary_id", nullable = false)
    private Dictionary dictionary;
    
    @Column(nullable = false, length = 100)
    @NotBlank(message = "Tiêu đề quiz không được để trống")
    @Size(max = 100, message = "Tiêu đề không được vượt quá 100 ký tự")
    private String title;
    
    @Column(length = 10)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.ACTIVE;
    
    @Column(name = "time_limit_sec")
    private Integer timeLimitSec; // Time limit in seconds
    
    @Column(name = "num_items")
    private Integer numItems; // Number of questions
    
    @Column(name = "pass_score")
    private Integer passScore; // Minimum score to pass (percentage)
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    // Relationships
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<QuizItems> quizItems;
    
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<QuizAttempts> quizAttempts;
    
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
