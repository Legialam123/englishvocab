package com.englishvocab.controller;

import com.englishvocab.dto.MediaResponseDto;
import com.englishvocab.entity.Media.EntityType;
import com.englishvocab.security.CustomUserPrincipal;
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
            // Get user ID from CustomUserPrincipal (UUID String)
            CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
            String userId = principal.getId();
            
            System.out.println("DEBUG: Uploading avatar for userId: " + userId + " (type: " + userId.getClass().getName() + ")");
            
            MediaResponseDto response = mediaService.uploadUserAvatar(file, userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("ERROR in uploadAvatar: " + e.getMessage());
            e.printStackTrace();
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
            CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
            String userId = principal.getId();
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
            CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
            String userId = principal.getId();
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
            CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
            String userId = principal.getId();
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
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
        String userId = principal.getId();
        Long totalUsed = mediaService.getTotalStorageUsed(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalUsed", totalUsed);
        response.put("quota", 104857600L); // 100MB
        response.put("remaining", 104857600L - totalUsed);
        response.put("percentUsed", (totalUsed * 100.0) / 104857600L);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Admin endpoint: Cleanup orphaned files
     * DELETE /api/media/cleanup/orphaned
     */
    @DeleteMapping("/cleanup/orphaned")
    public ResponseEntity<Map<String, Object>> cleanupOrphanedFiles(Authentication authentication) {
        try {
            // Check if user is admin
            CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
            if (principal.getRole() != com.englishvocab.entity.User.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            int deletedCount = mediaService.cleanupOrphanedFiles();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Cleanup completed successfully");
            response.put("deletedFiles", deletedCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("ERROR in cleanupOrphanedFiles: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to cleanup orphaned files");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
