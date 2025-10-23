package com.englishvocab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for adding multiple vocabularies to lists
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddToListsRequest {
    
    /**
     * Vocabulary ID (for system vocab)
     */
    private Integer vocabId;
    
    /**
     * Custom Vocabulary ID (for custom vocab)
     */
    private Integer customVocabId;
    
    /**
     * List of list IDs to add the vocabulary to
     */
    private List<Integer> listIds;
    
    /**
     * Optional: create a new list with this name and add vocab to it
     */
    private String newListName;
    
    /**
     * Optional: description for the new list
     */
    private String newListDescription;
}
