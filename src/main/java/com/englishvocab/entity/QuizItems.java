package com.englishvocab.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "quiz_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizItems {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_item_id")
    private Integer quizItemId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_vocab_id")
    private UserCustomVocab customVocab;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quizzes quiz;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocab_id")
    private Vocab vocab;
    
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Type type = Type.MULTIPLE_CHOICE;
    
    @Column(nullable = false, length = 200)
    @NotBlank(message = "Câu hỏi không được để trống")
    @Size(max = 200, message = "Câu hỏi không được vượt quá 200 ký tự")
    private String prompt; // The question
    
    @Column(length = 200)
    @Size(max = 200, message = "Lựa chọn không được vượt quá 200 ký tự")
    private String option; // Options (JSON string for multiple choice)
    
    @Column(nullable = false, length = 100)
    @NotBlank(message = "Đáp án không được để trống")
    @Size(max = 100, message = "Đáp án không được vượt quá 100 ký tự")
    private String answer; // Correct answer
    
    @Column(length = 200)
    @Size(max = 200, message = "Giải thích không được vượt quá 200 ký tự")
    private String explanation;
    
    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Difficulty difficulty = Difficulty.MEDIUM;
    
    // Relationships
    @OneToMany(mappedBy = "quizItem", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<QuizItemResults> results;
    
    public enum Type {
        MULTIPLE_CHOICE,    // Trắc nghiệm
        FILL_IN_BLANK,     // Điền vào chỗ trống
        TRUE_FALSE,        // Đúng/Sai
        MATCHING,          // Ghép đôi
        PRONUNCIATION      // Phát âm
    }
    
    public enum Difficulty {
        EASY, MEDIUM, HARD
    }
    
    /**
     * Convenience methods
     */
    public boolean isMultipleChoice() {
        return type == Type.MULTIPLE_CHOICE;
    }
    
    public String getTypeDisplayName() {
        return switch (type) {
            case MULTIPLE_CHOICE -> "Trắc nghiệm";
            case FILL_IN_BLANK -> "Điền từ";
            case TRUE_FALSE -> "Đúng/Sai";
            case MATCHING -> "Ghép đôi";
            case PRONUNCIATION -> "Phát âm";
        };
    }
    
    public String getDifficultyDisplayName() {
        return switch (difficulty) {
            case EASY -> "Dễ";
            case MEDIUM -> "Trung bình";
            case HARD -> "Khó";
        };
    }
    
    public String getSourceWord() {
        if (vocab != null) {
            return vocab.getDisplayWord();
        } else if (customVocab != null) {
            return customVocab.getDisplayWord();
        }
        return "N/A";
    }
}
