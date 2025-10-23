package com.englishvocab.dto;

import com.englishvocab.entity.SessionVocabulary;
import com.englishvocab.entity.Vocab;
import com.englishvocab.entity.Senses;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.stream.Collectors;

/**
 * DTO để truyền dữ liệu SessionVocabulary tới frontend
 * Giải quyết vấn đề LazyInitializationException khi serialize entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionVocabularyDTO {
    // SessionVocabulary fields
    private Long id;
    private Long sessionId;
    private String userAnswer; // Changed from answerLevel to userAnswer (AnswerType enum)
    
    // Vocabulary fields (flattened)
    private Long vocabId;
    private String word;
    private String ipa;
    private String pos;
    private String meaning; // Combined from all senses
    private String level;
    
    /**
     * Factory method để tạo DTO từ SessionVocabulary entity
     */
    public static SessionVocabularyDTO from(SessionVocabulary sv) {
        Vocab vocab = sv.getVocab();
        
        // Combine all sense meanings into one string
        String combinedMeaning = vocab.getSenses().stream()
            .map(sense -> {
                String meaningVi = sense.getMeaningVi() != null ? sense.getMeaningVi() : "";
                String definition = sense.getDefinition() != null ? " (" + sense.getDefinition() + ")" : "";
                return meaningVi + definition;
            })
            .filter(s -> !s.trim().isEmpty())
            .collect(Collectors.joining("; "));
        
        return SessionVocabularyDTO.builder()
            .id(sv.getSessionVocabId())
            .sessionId(sv.getSession().getSessionId())
            .userAnswer(sv.getUserAnswer() != null ? sv.getUserAnswer().toString() : null)
            .vocabId(vocab.getVocabId().longValue())
            .word(vocab.getWord())
            .ipa(vocab.getIpa())
            .pos(vocab.getPos())
            .meaning(combinedMeaning.isEmpty() ? "Chưa có nghĩa" : combinedMeaning)
            .level(vocab.getLevel() != null ? vocab.getLevel().toString() : "")
            .build();
    }
}
