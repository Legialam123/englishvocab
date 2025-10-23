package com.englishvocab.dto;

import com.englishvocab.entity.UserVocabList.Status;
import com.englishvocab.entity.UserVocabList.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing vocabulary list
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateListRequest {
    
    /**
     * Updated name of the list (required)
     */
    @NotBlank(message = "List name is required")
    @Size(max = 100, message = "List name cannot exceed 100 characters")
    private String name;
    
    /**
     * Updated description (optional)
     */
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;
    
    /**
     * Updated visibility setting
     */
    private Visibility visibility;
    
    /**
     * Updated status (ACTIVE, INACTIVE, ARCHIVED)
     */
    private Status status;
}
