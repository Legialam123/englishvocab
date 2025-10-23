package com.englishvocab.service;

import com.englishvocab.dto.MediaResponseDto;
import com.englishvocab.dto.MediaUploadDto;
import com.englishvocab.entity.Media;
import com.englishvocab.entity.Media.EntityType;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface MediaService {
    
    /**
     * Upload media tổng quát
     */
    MediaResponseDto uploadMedia(MediaUploadDto uploadDto, String currentUserId) throws IOException;
    
    /**
     * Upload avatar cho user
     */
    MediaResponseDto uploadUserAvatar(MultipartFile file, String userId) throws IOException;
    
    /**
     * Upload audio cho vocabulary
     */
    MediaResponseDto uploadVocabAudio(MultipartFile file, Integer vocabId, String userId) throws IOException;
    
    /**
     * Upload ảnh minh họa cho vocabulary
     */
    MediaResponseDto uploadVocabImage(MultipartFile file, Integer vocabId, String userId) throws IOException;
    
    /**
     * Get media by ID
     */
    Media getMediaById(Long mediaId);
    
    /**
     * Get media response DTO
     */
    MediaResponseDto getMediaResponseById(Long mediaId);
    
    /**
     * Get all media của entity
     */
    List<MediaResponseDto> getMediaByEntity(EntityType entityType, String entityId);
    
    /**
     * Get primary media của entity
     */
    MediaResponseDto getPrimaryMediaByEntity(EntityType entityType, String entityId);
    
    /**
     * Delete media
     */
    void deleteMedia(Long mediaId, String currentUserId) throws IOException;
    
    /**
     * Load file as Resource để serve
     */
    Resource loadFileAsResource(String fileName) throws IOException;
    
    /**
     * Get user avatar URL
     */
    String getUserAvatarUrl(String userId);
    
    /**
     * Get vocab audio URL
     */
    String getVocabAudioUrl(Integer vocabId);
    
    /**
     * Check user storage quota (giới hạn 100MB/user)
     */
    boolean checkUserStorageQuota(String userId, long fileSize);
    
    /**
     * Get total storage used by user
     */
    Long getTotalStorageUsed(String userId);
    
    /**
     * Convert entity to DTO
     */
    MediaResponseDto convertToDto(Media media);
    
    /**
     * Cleanup orphaned files (files in uploads folder but not in database)
     * Returns number of files deleted
     */
    int cleanupOrphanedFiles() throws IOException;
}
