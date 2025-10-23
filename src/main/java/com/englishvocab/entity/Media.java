package com.englishvocab.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "media", indexes = {
    @Index(name = "idx_entity", columnList = "entityType,entityId"),
    @Index(name = "idx_type", columnList = "mediaType")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class Media {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer mediaId;  // Changed from Long to Integer for consistency with other entities
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    MediaType mediaType;
    
    @Column(nullable = false)
    String fileName;
    
    @Column(nullable = false, length = 500)
    String filePath;
    
    Long fileSize; // in bytes
    
    @Column(length = 100)
    String mimeType; // e.g., "image/jpeg", "audio/mpeg"
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    User uploader;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    EntityType entityType;
    
    @Column(nullable = false, length = 255)
    String entityId;  // Changed to String to support both UUID (User) and Integer (Vocab)
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    Boolean isPrimary = false; // Primary media for entity (e.g., main avatar, default audio)
    
    @Column(columnDefinition = "TEXT")
    String metadata; // JSON string chứa metadata (dimensions, duration, etc.)
    
    String description;
    
    @CreationTimestamp
    @Column(updatable = false)
    LocalDateTime createdAt;
    
    @UpdateTimestamp
    LocalDateTime updatedAt;
    
    // Computed field để lấy URL đầy đủ
    @Transient
    public String getFullUrl() {
        return "/uploads/" + filePath;
    }
    
    // Helper method để check file type
    @Transient
    public boolean isImage() {
        return mediaType == MediaType.PROFILE_AVATAR || 
               mediaType == MediaType.VOCAB_IMAGE ||
               (mimeType != null && mimeType.startsWith("image/"));
    }
    
    @Transient
    public boolean isAudio() {
        return mediaType == MediaType.VOCAB_AUDIO ||
               (mimeType != null && mimeType.startsWith("audio/"));
    }
    
    @Transient
    public String getFormattedFileSize() {
        if (fileSize == null) return "0 B";
        
        long bytes = fileSize;
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    // Enums
    public enum MediaType {
        PROFILE_AVATAR("Ảnh đại diện"),
        VOCAB_AUDIO("Audio từ vựng"),
        VOCAB_IMAGE("Ảnh minh họa từ vựng"),
        DICTIONARY_COVER("Ảnh bìa từ điển");
        
        private final String displayName;
        
        MediaType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum EntityType {
        USER("Người dùng"),
        VOCABULARY("Từ vựng"),
        DICTIONARY("Từ điển"),
        TOPIC("Chủ đề");
        
        private final String displayName;
        
        EntityType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
