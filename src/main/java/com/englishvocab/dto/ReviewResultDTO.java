package com.englishvocab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResultDTO {
    private int totalCorrect;
    private int totalQuestions;
    private List<String> masteredWords;
    private List<String> needReviewWords;
    private Duration duration;
    
    public double getPercentageScore() {
        if (totalQuestions == 0) return 0.0;
        return (totalCorrect * 100.0) / totalQuestions;
    }
    
    public String getFormattedScore() {
        return totalCorrect + "/" + totalQuestions + " (" + String.format("%.1f", getPercentageScore()) + "%)";
    }
    
    public String getFormattedDuration() {
        if (duration == null) return "N/A";
        
        long minutes = duration.toMinutes();
        long seconds = duration.getSeconds() % 60;
        
        if (minutes > 0) {
            return minutes + " phút " + seconds + " giây";
        } else {
            return seconds + " giây";
        }
    }
}
