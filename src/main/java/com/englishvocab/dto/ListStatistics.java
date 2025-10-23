package com.englishvocab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Statistics DTO for a vocabulary list
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListStatistics {
    
    /**
     * List ID
     */
    private Integer listId;
    
    /**
     * List name
     */
    private String listName;
    
    /**
     * Count of system vocabularies in the list
     */
    private long systemVocabCount;
    
    /**
     * Count of custom vocabularies in the list
     */
    private long customVocabCount;
    
    /**
     * Total vocabulary count (system + custom)
     */
    private long totalVocabCount;
}
