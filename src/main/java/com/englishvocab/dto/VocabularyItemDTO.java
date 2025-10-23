package com.englishvocab.dto;

import com.englishvocab.entity.UserCustomVocab;
import com.englishvocab.entity.Vocab;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Unified DTO for representing vocabulary items in a list.
 * Can represent both system vocabulary (Vocab) and custom vocabulary (UserCustomVocab).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyItemDTO {
    
    /**
     * Type of vocabulary: "SYSTEM" or "CUSTOM"
     */
    private String type;
    
    /**
     * ID for system vocabulary (null if custom)
     */
    private Integer vocabId;
    
    /**
     * ID for custom vocabulary (null if system)
     */
    private Integer customVocabId;
    
    /**
     * The word itself
     */
    private String word;
    
    /**
     * International Phonetic Alphabet pronunciation
     */
    private String ipa;
    
    /**
     * Part of speech (noun, verb, adjective, etc.)
     */
    private String pos;
    
    /**
     * Vietnamese meaning
     */
    private String meaning;
    
    /**
     * English definition (only for system vocab)
     */
    private String definition;
    
    /**
     * Example sentence (only for system vocab)
     */
    private String example;
    
    /**
     * CEFR level (A1, A2, B1, B2, C1, C2) - only for system vocab
     */
    private String level;
    
    /**
     * When this vocab was added to the list
     */
    private LocalDateTime addedAt;
    
    /**
     * Create DTO from system Vocab entity
     */
    public static VocabularyItemDTO fromSystemVocab(Vocab vocab, LocalDateTime addedAt) {
        return VocabularyItemDTO.builder()
                .type("SYSTEM")
                .vocabId(vocab.getVocabId())
                .word(vocab.getWord())
                .ipa(vocab.getIpa())
                .pos(vocab.getPos())
                .meaning(vocab.getPrimarySense())
                .definition(vocab.getDefinition())
                .example(vocab.getExample())
                .level(vocab.getLevel() != null ? vocab.getLevel().name() : "BEGINNER")
                .addedAt(addedAt)
                .build();
    }
    
    /**
     * Create DTO from custom UserCustomVocab entity
     */
    public static VocabularyItemDTO fromCustomVocab(UserCustomVocab customVocab, LocalDateTime addedAt) {
        return VocabularyItemDTO.builder()
                .type("CUSTOM")
                .customVocabId(customVocab.getCustomVocabId())
                .word(customVocab.getName())
                .ipa(customVocab.getIpa())
                .pos(customVocab.getPos())
                .meaning(customVocab.getMeaningVi())
                .addedAt(addedAt != null ? addedAt : customVocab.getCreatedAt())
                .build();
    }
    
    /**
     * Check if this is a system vocabulary
     */
    public boolean isSystemVocab() {
        return "SYSTEM".equals(type);
    }
    
    /**
     * Check if this is a custom vocabulary
     */
    public boolean isCustomVocab() {
        return "CUSTOM".equals(type);
    }
}
