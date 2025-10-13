package com.englishvocab.controller;

import com.englishvocab.dto.MediaResponseDto;
import com.englishvocab.entity.Media.EntityType;
import com.englishvocab.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {
    
    private final MediaService mediaService;
    
    /**
     * Upload avatar cho user hiện tại
     */
    @PostMapping("/avatar/upload")
    public ResponseEntity<MediaResponseDto> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            String userId = authentication.getName();  // userId is String (UUID)
            MediaResponseDto response = mediaService.uploadUserAvatar(file, userId);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Upload audio cho vocabulary
     */
    @PostMapping("/vocab/{vocabId}/audio")
    public ResponseEntity<MediaResponseDto> uploadVocabAudio(
            @PathVariable Integer vocabId,  // Changed from Long to Integer
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            MediaResponseDto response = mediaService.uploadVocabAudio(file, vocabId, userId);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Upload ảnh minh họa cho vocabulary
     */
    @PostMapping("/vocab/{vocabId}/image")
    public ResponseEntity<MediaResponseDto> uploadVocabImage(
            @PathVariable Integer vocabId,  // Changed from Long to Integer
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            MediaResponseDto response = mediaService.uploadVocabImage(file, vocabId, userId);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get media by ID
     */
    @GetMapping("/{mediaId}")
    public ResponseEntity<MediaResponseDto> getMedia(@PathVariable Long mediaId) {
        MediaResponseDto response = mediaService.getMediaResponseById(mediaId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all media của entity
     */
    @GetMapping("/{entityType}/{entityId}")
    public ResponseEntity<List<MediaResponseDto>> getMediaByEntity(
            @PathVariable EntityType entityType,
            @PathVariable String entityId) {  // Changed from Long to String
        List<MediaResponseDto> mediaList = mediaService.getMediaByEntity(entityType, entityId);
        return ResponseEntity.ok(mediaList);
    }
    
    /**
     * Download/serve file
     */
    @GetMapping("/files/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            Resource resource = mediaService.loadFileAsResource(fileName);
            
            String contentType = "application/octet-stream";
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"");
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .headers(headers)
                .body(resource);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Delete media
     */
    @DeleteMapping("/{mediaId}")
    public ResponseEntity<Map<String, String>> deleteMedia(
            @PathVariable Long mediaId,
            Authentication authentication) {
        try {
            String userId = authentication.getName();  // userId is String (UUID)
            mediaService.deleteMedia(mediaId, userId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Xóa media thành công");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get storage info
     */
    @GetMapping("/storage/info")
    public ResponseEntity<Map<String, Object>> getStorageInfo(Authentication authentication) {
        String userId = authentication.getName();  // userId is String (UUID)
        Long totalUsed = mediaService.getTotalStorageUsed(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalUsed", totalUsed);
        response.put("quota", 104857600L); // 100MB
        response.put("remaining", 104857600L - totalUsed);
        response.put("percentUsed", (totalUsed * 100.0) / 104857600L);
        
        return ResponseEntity.ok(response);
    }
}
