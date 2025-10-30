package com.englishvocab.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Entity
@Table(name = "review_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class ReviewItems {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_item_id")
    Integer reviewItemId;
    
    @Column(name = "session_order")
    Integer sessionOrder; // Order in the review session
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_vocab_id")
    UserCustomVocab customVocab;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    Reviews review;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocab_id")
    Vocab vocab;
    
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    Type type = Type.MULTIPLE_CHOICE;
    
    @Column(nullable = false, length = 200)
    @NotBlank(message = "Câu hỏi không được để trống")
    @Size(max = 200, message = "Câu hỏi không được vượt quá 200 ký tự")
    String prompt; // The question
    
    @Column(length = 200)
    @Size(max = 200, message = "Lựa chọn không được vượt quá 200 ký tự")
    String option; // Options (JSON string for multiple choice)
    
    @Column(nullable = false, length = 100)
    @NotBlank(message = "Đáp án không được để trống")
    @Size(max = 100, message = "Đáp án không được vượt quá 100 ký tự")
    String answer; // Correct answer
    
    @Column(length = 200)
    @Size(max = 200, message = "Giải thích không được vượt quá 200 ký tự")
    String explanation;
    
    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    Difficulty difficulty = Difficulty.MEDIUM;
    
    // Relationships
    @OneToMany(mappedBy = "reviewItem", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<ReviewItemResults> results;
    
    public enum Type {
        MULTIPLE_CHOICE,    // Trắc nghiệm
        FILL_IN_BLANK,      // Điền vào chỗ trống
        TRUE_FALSE,         // Đúng/Sai
        MATCHING,           // Ghép đôi
        PRONUNCIATION,      // Phát âm
        TYPING              // Đánh máy (cho review)
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
    
    public boolean isTrueFalse() {
        return type == Type.TRUE_FALSE;
    }
    
    public boolean isFillInBlank() {
        return type == Type.FILL_IN_BLANK;
    }
    
    public boolean isTyping() {
        return type == Type.TYPING;
    }
    
    public String getTypeDisplayName() {
        return switch (type) {
            case MULTIPLE_CHOICE -> "Trắc nghiệm";
            case FILL_IN_BLANK -> "Điền từ";
            case TRUE_FALSE -> "Đúng/Sai";
            case MATCHING -> "Ghép đôi";
            case PRONUNCIATION -> "Phát âm";
            case TYPING -> "Đánh máy";
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
    
    /**
     * Get options as array for multiple choice
     */
    public String[] getOptionsArray() {
        if (option == null || option.isEmpty()) return new String[0];
        return option.split("\\|");
    }
    
    /**
     * Set options from array for multiple choice
     */
    public void setOptionsArray(String[] options) {
        this.option = String.join("|", options);
    }
}
