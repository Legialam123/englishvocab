package com.englishvocab.dto;

import com.englishvocab.entity.Media.EntityType;
import com.englishvocab.entity.Media.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaResponseDto {
    
    private Integer mediaId;
    private MediaType mediaType;
    private String fileName;
    private String fullUrl;
    private Long fileSize;
    private String formattedFileSize;
    private String mimeType;
    private EntityType entityType;
    private String entityId;  // Changed to String
    private Boolean isPrimary;
    private String description;
    private String metadata;
    private String uploaderId;  // Changed to String (UUID)
    private String uploaderName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
