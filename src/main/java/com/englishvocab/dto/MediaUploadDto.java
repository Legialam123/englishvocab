package com.englishvocab.dto;

import com.englishvocab.entity.Media.EntityType;
import com.englishvocab.entity.Media.MediaType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaUploadDto {
    
    @NotNull(message = "File không được để trống")
    private MultipartFile file;
    
    @NotNull(message = "Loại media không được để trống")
    private MediaType mediaType;
    
    @NotNull(message = "Loại entity không được để trống")
    private EntityType entityType;
    
    @NotNull(message = "Entity ID không được để trống")
    private String entityId;  // Changed to String to support UUID and Integer
    
    private Boolean isPrimary;
    
    private String description;
    
    private String metadata;
}
