package com.englishvocab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO cho request hoàn thành session
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionResultRequest {
    
    private String sessionId; // UUID
    private Integer duration; // Tổng thời gian (giây)
    private List<VocabAnswer> answers; // Danh sách câu trả lời
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VocabAnswer {
        private Integer vocabId;
        private String answerType; // "CORRECT", "WRONG", "SKIP"
        private Integer timeSpent; // Thời gian trả lời từ này (giây)
    }
}
