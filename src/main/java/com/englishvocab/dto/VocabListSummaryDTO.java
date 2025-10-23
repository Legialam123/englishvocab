package com.englishvocab.dto;

import com.englishvocab.entity.UserVocabList;
import com.englishvocab.entity.UserVocabList.Status;
import com.englishvocab.entity.UserVocabList.Visibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Summary DTO for vocabulary list (used in list view)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabListSummaryDTO {
    
    private Integer listId;
    private String name;
    private String description;
    private Visibility visibility;
    private Status status;
    
    /**
     * Dictionary info (if dictionary-based)
     */
    private Integer dictionaryId;
    private String dictionaryName;
    
    /**
     * Vocabulary counts
     */
    private long systemVocabCount;
    private long customVocabCount;
    private long totalVocabCount;
    
    /**
     * List type: "Dictionary-based" or "Custom-only"
     */
    private String listType;
    
    /**
     * Timestamps
     */
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Create summary DTO from entity (counts provided separately)
     */
    public static VocabListSummaryDTO from(UserVocabList list, long systemCount, long customCount) {
        return VocabListSummaryDTO.builder()
                .listId(list.getUserVocabListId())
                .name(list.getName())
                .description(list.getDescription())
                .visibility(list.getVisibility())
                .status(list.getStatus())
                .systemVocabCount(systemCount)
                .customVocabCount(customCount)
                .totalVocabCount(list.getTotalVocabCount())
                .createdAt(list.getCreatedAt())
                .updatedAt(list.getUpdatedAt())
                .build();
    }
}
