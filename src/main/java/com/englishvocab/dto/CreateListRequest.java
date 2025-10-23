package com.englishvocab.dto;

import com.englishvocab.entity.UserVocabList.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new vocabulary list
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateListRequest {
    
    /**
     * Name of the list (required, max 100 characters)
     */
    @NotBlank(message = "List name is required")
    @Size(max = 100, message = "List name cannot exceed 100 characters")
    private String name;
    
    /**
     * Description of the list (optional, max 255 characters)
     */
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;
    
    /**
     * Visibility setting (default: PRIVATE)
     */
    @Builder.Default
    private Visibility visibility = Visibility.PRIVATE;
}
