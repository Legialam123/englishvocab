package com.englishvocab.dto;

import com.englishvocab.entity.LearningSession;
import com.englishvocab.entity.SessionVocabulary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO cho kết quả session
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionResultDTO {
    
    private String sessionUuid;
    private Integer dictionaryId;
    private String dictionaryName;
    private String learningMode;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer timeSpentSec;
    private String formattedDuration;
    
    // Statistics
    private Integer totalWords;
    private Integer correctCount;
    private Integer wrongCount;
    private Integer skipCount;
    private Double accuracyPercentage;
    
    // Vocabulary details
    private List<VocabResult> vocabularies;
    
    // Achievements/Badges
    private List<String> achievements;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VocabResult {
        private Integer vocabId;
        private String word;
        private String meaning;
        private String ipa;
        private String userAnswer; // CORRECT, WRONG, SKIP
        private String answerIcon;
        private String answerCssClass;
        private Integer timeSpent;
    }
    
    /**
     * Factory method để tạo từ entities
     */
    public static SessionResultDTO fromSession(LearningSession session, List<SessionVocabulary> vocabularies) {
        SessionResultDTOBuilder builder = SessionResultDTO.builder()
                .sessionUuid(session.getSessionUuid())
                .dictionaryId(session.getDictionary().getDictionaryId())
                .dictionaryName(session.getDictionary().getName())
                .learningMode(session.getLearningMode().name())
                .startedAt(session.getStartedAt())
                .completedAt(session.getCompletedAt())
                .timeSpentSec(session.getTimeSpentSec())
                .formattedDuration(session.getFormattedDuration())
                .totalWords(session.getActualWords())
                .correctCount(session.getCorrectCount())
                .wrongCount(session.getWrongCount())
                .skipCount(session.getSkipCount())
                .accuracyPercentage(session.getAccuracyPercentage());
        
        // Convert vocabularies
        List<VocabResult> vocabResults = vocabularies.stream()
                .map(sv -> {
                    // Get primary meaning from first sense
                    String meaning = sv.getVocab().getSenses() != null && !sv.getVocab().getSenses().isEmpty()
                        ? sv.getVocab().getSenses().get(0).getMeaningVi()
                        : "";
                    
                    return VocabResult.builder()
                        .vocabId(sv.getVocab().getVocabId())
                        .word(sv.getVocab().getWord())
                        .meaning(meaning)
                        .ipa(sv.getVocab().getIpa())
                        .userAnswer(sv.getUserAnswer() != null ? sv.getUserAnswer().name() : null)
                        .answerIcon(sv.getAnswerIcon())
                        .answerCssClass(sv.getAnswerCssClass())
                        .timeSpent(sv.getTimeSpentSec())
                        .build();
                })
                .toList();
        
        builder.vocabularies(vocabResults);
        
        // Calculate achievements
        List<String> achievements = calculateAchievements(session);
        builder.achievements(achievements);
        
        return builder.build();
    }
    
    private static List<String> calculateAchievements(LearningSession session) {
        List<String> badges = new java.util.ArrayList<>();
        
        double accuracy = session.getAccuracyPercentage();
        if (accuracy == 100.0) {
            badges.add("Perfect Score");
        } else if (accuracy >= 90.0) {
            badges.add("Excellent");
        } else if (accuracy >= 75.0) {
            badges.add("Good Job");
        }
        
        if (session.getTimeSpentSec() != null && session.getActualWords() > 0) {
            double avgTime = session.getTimeSpentSec() / (double) session.getActualWords();
            if (avgTime < 10) {
                badges.add("Speed Learner");
            }
        }
        
        if (session.getSkipCount() == 0) {
            badges.add("No Skip Challenge");
        }
        
        return badges;
    }
}
