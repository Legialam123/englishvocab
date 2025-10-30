package com.englishvocab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    
    /**
     * Tổng số từ vựng (system + custom) trong tất cả danh sách
     */
    private long totalVocabCount;
    
    /**
     * Số lượng danh sách từ vựng
     */
    private long totalListCount;
    
    /**
     * Số ngày học liên tục (streak) - placeholder for future implementation
     */
    private int studyStreak;
    
    /**
     * Số từ vựng đã thành thạo (mastered) - placeholder for future implementation
     */
    private long masteredVocabCount;
}

