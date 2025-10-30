package com.englishvocab.dto;

import com.englishvocab.entity.Vocab;
import com.englishvocab.entity.UserVocabProgress;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO để truyền thông tin Vocab kèm tiến độ học tập của user
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabWithProgressDTO {
    private Vocab vocab;
    private Integer proficiency; // 0-100% (calculated from box and status)
    private UserVocabProgress.Status status;
    private Integer box;
    private Integer streak;
    private Integer wrongCount;
    
    /**
     * Factory method để tạo DTO từ Vocab và UserVocabProgress
     */
    public static VocabWithProgressDTO of(Vocab vocab, UserVocabProgress progress) {
        if (progress == null) {
            // Chưa có tiến độ - từ mới
            return VocabWithProgressDTO.builder()
                .vocab(vocab)
                .proficiency(0)
                .status(UserVocabProgress.Status.NEW)
                .box(0)
                .streak(0)
                .wrongCount(0)
                .build();
        }
        
        // Tính proficiency dựa trên box và status
        int proficiency = calculateProficiency(progress.getBox(), progress.getStatus());
        
        return VocabWithProgressDTO.builder()
            .vocab(vocab)
            .proficiency(proficiency)
            .status(progress.getStatus())
            .box(progress.getBox())
            .streak(progress.getStreak())
            .wrongCount(progress.getWrongCount())
            .build();
    }
    
    /**
     * Tính proficiency (0-100%) từ box và status
     */
    private static int calculateProficiency(Integer box, UserVocabProgress.Status status) {
        if (box == null || box == 0) {
            return 0;
        }
        
        // Base proficiency từ box (1-5)
        int baseProficiency = switch (box) {
            case 1 -> 20;  // Box 1: 20%
            case 2 -> 40;  // Box 2: 40%
            case 3 -> 60;  // Box 3: 60%
            case 4 -> 80;  // Box 4: 80%
            case 5 -> 100; // Box 5: 100%
            default -> 0;
        };
        
        // Điều chỉnh dựa trên status
        if (status == UserVocabProgress.Status.MASTERED) {
            return Math.max(baseProficiency, 80); // Mastered: ít nhất 80%
        } else if (status == UserVocabProgress.Status.DIFFICULT) {
            return Math.min(baseProficiency, 30); // Difficult: tối đa 30%
        }
        
        return baseProficiency;
    }
    
    // Convenience methods for review system
    public String getWord() {
        return vocab != null ? vocab.getWord() : "";
    }
    
    public String getPrimaryMeaning() {
        return vocab != null ? vocab.getPrimaryMeaning() : "";
    }
    
    public String getIpa() {
        return vocab != null ? vocab.getIpa() : "";
    }
}
