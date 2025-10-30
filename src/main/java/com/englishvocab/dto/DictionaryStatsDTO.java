package com.englishvocab.dto;

import com.englishvocab.entity.Dictionary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for dictionary with statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictionaryStatsDTO {
    
    private Integer dictionaryId;
    private String name;
    private String description;
    private String code;
    private String publisher;
    private Dictionary.Status status;
    
    /**
     * Statistics
     */
    private long totalVocabCount;      // Tổng số từ vựng trong từ điển
    private long totalLearners;        // Số người đang học từ điển này
    
    /**
     * Create from Dictionary entity with stats
     */
    public static DictionaryStatsDTO from(Dictionary dictionary, long vocabCount, long learners) {
        return DictionaryStatsDTO.builder()
                .dictionaryId(dictionary.getDictionaryId())
                .name(dictionary.getName())
                .description(dictionary.getDescription())
                .code(dictionary.getCode())
                .publisher(dictionary.getPublisher())
                .status(dictionary.getStatus())
                .totalVocabCount(vocabCount)
                .totalLearners(learners)
                .build();
    }
}

