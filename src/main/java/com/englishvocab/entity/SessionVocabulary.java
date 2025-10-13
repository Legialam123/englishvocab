package com.englishvocab.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity liên kết giữa Learning Session và Vocabulary
 * Lưu trữ thông tin về từng từ vựng trong session
 */
@Entity
@Table(name = "session_vocabularies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class SessionVocabulary {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_vocab_id")
    Long sessionVocabId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @JsonIgnore
    LearningSession session;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocab_id", nullable = false)
    @JsonIgnoreProperties({"dictionary", "vocabTopics", "userProgress", "listVocabs", "hibernateLazyInitializer", "handler"})
    Vocab vocab;
    
    @Column(name = "order_index", nullable = false)
    Integer orderIndex; // Thứ tự trong session
    
    @Enumerated(EnumType.STRING)
    @Column(name = "user_answer", length = 20)
    AnswerType userAnswer;
    
    @Column(name = "time_spent_sec")
    Integer timeSpentSec; // Thời gian trả lời từ này (giây)
    
    @Column(name = "answered_at")
    LocalDateTime answeredAt;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
    
    /**
     * Enum cho loại câu trả lời
     */
    public enum AnswerType {
        CORRECT,    // Trả lời đúng (Đã nhớ)
        WRONG,      // Trả lời sai (Chưa biết / Nhớ tạm)
        SKIP        // Bỏ qua
    }
    
    /**
     * Helper methods
     */
    
    public boolean isAnswered() {
        return userAnswer != null;
    }
    
    public boolean isCorrect() {
        return userAnswer == AnswerType.CORRECT;
    }
    
    public boolean isWrong() {
        return userAnswer == AnswerType.WRONG;
    }
    
    public boolean isSkipped() {
        return userAnswer == AnswerType.SKIP;
    }
    
    public void recordAnswer(AnswerType answer, Integer timeSpent) {
        this.userAnswer = answer;
        this.timeSpentSec = timeSpent;
        this.answeredAt = LocalDateTime.now();
    }
    
    public String getAnswerDisplayName() {
        if (userAnswer == null) return "Chưa trả lời";
        return switch (userAnswer) {
            case CORRECT -> "Đúng";
            case WRONG -> "Sai";
            case SKIP -> "Bỏ qua";
        };
    }
    
    public String getAnswerIcon() {
        if (userAnswer == null) return "⏳";
        return switch (userAnswer) {
            case CORRECT -> "✅";
            case WRONG -> "❌";
            case SKIP -> "⏭️";
        };
    }
    
    public String getAnswerCssClass() {
        if (userAnswer == null) return "text-muted";
        return switch (userAnswer) {
            case CORRECT -> "text-success";
            case WRONG -> "text-danger";
            case SKIP -> "text-warning";
        };
    }
}
