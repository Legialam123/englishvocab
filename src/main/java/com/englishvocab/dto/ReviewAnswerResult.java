package com.englishvocab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewAnswerResult {
    // Common fields
    private boolean correct;
    private String userAnswer;
    private String explanation;
    private String questionType;
    private String word;
    private String meaning;
    
    // Multiple Choice specific
    private Integer correctOptionIndex;
    private String correctOptionText;
    
    // True/False specific  
    private String correctTrueFalse;
    
    // Fill-in-Blank specific
    private String correctWordAnswer;
    
    // Legacy field for backward compatibility (deprecated)
    @Deprecated
    private String correctAnswer;
    
    /**
     * Create result for Multiple Choice question
     */
    public static ReviewAnswerResult multipleChoice(boolean correct, String userAnswer, 
                                                   Integer correctIndex, String correctText,
                                                   String word, String meaning) {
        return ReviewAnswerResult.builder()
            .correct(correct)
            .userAnswer(userAnswer)
            .questionType("MULTIPLE_CHOICE")
            .word(word)
            .meaning(meaning)
            .correctOptionIndex(correctIndex)
            .correctOptionText(correctText)
            .explanation(correct ? "Chính xác! 🎉" : "Sai rồi! Đáp án đúng là: " + correctText)
            .build();
    }
    
    /**
     * Create result for True/False question
     */
    public static ReviewAnswerResult trueFalse(boolean correct, String userAnswer,
                                              String correctAnswer, String word, String meaning) {
        return ReviewAnswerResult.builder()
            .correct(correct)
            .userAnswer(userAnswer)
            .questionType("TRUE_FALSE")
            .word(word)
            .meaning(meaning)
            .correctTrueFalse(correctAnswer)
            .explanation(correct ? "Chính xác! 🎉" : "Sai rồi! Đáp án đúng là: " + correctAnswer)
            .build();
    }
    
    /**
     * Create result for Fill-in-Blank question
     */
    public static ReviewAnswerResult fillInBlank(boolean correct, String userAnswer,
                                                String correctWord, String word, String meaning) {
        return ReviewAnswerResult.builder()
            .correct(correct)
            .userAnswer(userAnswer)
            .questionType("FILL_IN_BLANK")
            .word(word)
            .meaning(meaning)
            .correctWordAnswer(correctWord)
            .explanation(correct ? "Chính xác! 🎉" : "Sai rồi! Đáp án đúng là: " + correctWord)
            .build();
    }
    
    // Legacy methods for backward compatibility
    @Deprecated
    public static ReviewAnswerResult correct(String correctAnswer, String userAnswer, String questionType, String word, String meaning) {
        return ReviewAnswerResult.builder()
            .correct(true)
            .correctAnswer(correctAnswer)
            .userAnswer(userAnswer)
            .explanation("Chính xác! 🎉")
            .questionType(questionType)
            .word(word)
            .meaning(meaning)
            .build();
    }
    
    @Deprecated
    public static ReviewAnswerResult incorrect(String correctAnswer, String userAnswer, String questionType, String word, String meaning) {
        return ReviewAnswerResult.builder()
            .correct(false)
            .correctAnswer(correctAnswer)
            .userAnswer(userAnswer)
            .explanation("Sai rồi! Đáp án đúng là: " + correctAnswer)
            .questionType(questionType)
            .word(word)
            .meaning(meaning)
            .build();
    }
}
