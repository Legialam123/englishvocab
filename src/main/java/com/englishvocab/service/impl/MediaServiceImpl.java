package com.englishvocab.service.impl;

import com.englishvocab.dto.MediaResponseDto;
import com.englishvocab.dto.MediaUploadDto;
import com.englishvocab.entity.Media;
import com.englishvocab.entity.Media.EntityType;
import com.englishvocab.entity.Media.MediaType;
import com.englishvocab.entity.User;
import com.englishvocab.repository.MediaRepository;
import com.englishvocab.repository.UserRepository;
import com.englishvocab.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MediaServiceImpl implements MediaService {
    
    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;
    
    @Value("${media.upload.dir:uploads}")
    private String uploadDir;
    
    @Value("${media.max.file.size:10485760}") // 10MB default
    private long maxFileSize;
    
    @Value("${media.user.quota:104857600}") // 100MB default per user
    private long userStorageQuota;
    
    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );
    
    private static final List<String> ALLOWED_AUDIO_TYPES = List.of(
        "audio/mpeg", "audio/mp3", "audio/wav", "audio/ogg"
    );
    
    @Override
    @Transactional
    public MediaResponseDto uploadMedia(MediaUploadDto uploadDto, String currentUserId) throws IOException {
        MultipartFile file = uploadDto.getFile();
        String savedFilePath = null;
        
        try {
            // Validate file
            validateFile(file, uploadDto.getMediaType());
            
            // Check storage quota
            if (!checkUserStorageQuota(currentUserId, file.getSize())) {
                throw new RuntimeException("Bạn đã vượt quá dung lượng lưu trữ cho phép (100MB)");
            }
            
            // Get user by ID (UUID String from CustomUserPrincipal)
            User uploader = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
            
            // Nếu isPrimary = true, set tất cả media cũ thành not primary
            if (Boolean.TRUE.equals(uploadDto.getIsPrimary())) {
                mediaRepository.unsetAllPrimaryForEntity(
                    uploadDto.getEntityType(), 
                    uploadDto.getEntityId()
                );
            }
            
            // Nếu là avatar, xóa avatar cũ
            if (uploadDto.getMediaType() == MediaType.PROFILE_AVATAR) {
                deleteOldMediaForEntity(uploadDto.getEntityType(), uploadDto.getEntityId(), MediaType.PROFILE_AVATAR);
            }
            
            // Save file to disk
            savedFilePath = saveFile(file, uploadDto.getMediaType());
            
            // Create media entity
            Media media = Media.builder()
                .mediaType(uploadDto.getMediaType())
                .fileName(file.getOriginalFilename())
                .filePath(savedFilePath)
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .uploader(uploader)
                .entityType(uploadDto.getEntityType())
                .entityId(uploadDto.getEntityId())
                .isPrimary(uploadDto.getIsPrimary() != null ? uploadDto.getIsPrimary() : false)
                .description(uploadDto.getDescription())
                .metadata(uploadDto.getMetadata())
                .build();
            
            // Save to database
            media = mediaRepository.save(media);
            
            log.info("Uploaded media: {} for {} ID: {}", media.getFileName(), 
                uploadDto.getEntityType(), uploadDto.getEntityId());
            
            // Convert to DTO
            return convertToDto(media);
            
        } catch (Exception e) {
            // Rollback: Delete file if it was saved but database operation failed
            if (savedFilePath != null) {
                try {
                    deleteFileFromDisk(savedFilePath);
                    log.warn("Rolled back file upload: {} due to error: {}", savedFilePath, e.getMessage());
                } catch (IOException cleanupEx) {
                    log.error("Failed to cleanup file after error: {}", savedFilePath, cleanupEx);
                }
            }
            
            // Re-throw the exception
            if (e instanceof IOException) {
                throw (IOException) e;
            } else if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException("Error uploading media", e);
            }
        }
    }
    
    @Override
    public MediaResponseDto uploadUserAvatar(MultipartFile file, String userId) throws IOException {
        MediaUploadDto uploadDto = MediaUploadDto.builder()
            .file(file)
            .mediaType(MediaType.PROFILE_AVATAR)
            .entityType(EntityType.USER)
            .entityId(userId)  // userId is already String, no conversion needed
            .isPrimary(true)
            .build();
        
        return uploadMedia(uploadDto, userId);
    }
    
    @Override
    public MediaResponseDto uploadVocabAudio(MultipartFile file, Integer vocabId, String userId) throws IOException {
        MediaUploadDto uploadDto = MediaUploadDto.builder()
            .file(file)
            .mediaType(MediaType.VOCAB_AUDIO)
            .entityType(EntityType.VOCABULARY)
            .entityId(String.valueOf(vocabId))  // Convert Integer to String
            .isPrimary(true)
            .build();
        
        return uploadMedia(uploadDto, userId);
    }
    
    @Override
    public MediaResponseDto uploadVocabImage(MultipartFile file, Integer vocabId, String userId) throws IOException {
        MediaUploadDto uploadDto = MediaUploadDto.builder()
            .file(file)
            .mediaType(MediaType.VOCAB_IMAGE)
            .entityType(EntityType.VOCABULARY)
            .entityId(String.valueOf(vocabId))  // Convert Integer to String
            .isPrimary(false)
            .build();
        
        return uploadMedia(uploadDto, userId);
    }
    
    @Override
    public Media getMediaById(Long mediaId) {
        return mediaRepository.findById(mediaId)
            .orElseThrow(() -> new RuntimeException("Media không tồn tại"));
    }
    
    @Override
    public MediaResponseDto getMediaResponseById(Long mediaId) {
        Media media = getMediaById(mediaId);
        return convertToDto(media);
    }
    
    @Override
    public List<MediaResponseDto> getMediaByEntity(EntityType entityType, String entityId) {
        List<Media> mediaList = mediaRepository.findByEntityTypeAndEntityId(entityType, entityId);
        return mediaList.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public MediaResponseDto getPrimaryMediaByEntity(EntityType entityType, String entityId) {
        return mediaRepository.findByEntityTypeAndEntityIdAndIsPrimaryTrue(entityType, entityId)
            .map(this::convertToDto)
            .orElse(null);
    }
    
    @Override
    public void deleteMedia(Long mediaId, String currentUserId) throws IOException {
        Media media = getMediaById(mediaId);
        
        // Check permission (chỉ người upload mới được xóa)
        if (!media.getUploader().getId().equals(currentUserId)) {
            throw new RuntimeException("Bạn không có quyền xóa media này");
        }
        
        // Delete file from disk
        deleteFileFromDisk(media.getFilePath());
        
        // Delete from database
        mediaRepository.delete(media);
        
        log.info("Deleted media: {} by user: {}", mediaId, currentUserId);
    }
    
    @Override
    public Resource loadFileAsResource(String fileName) throws IOException {
        try {
            Path filePath = Paths.get(uploadDir).resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File không tồn tại: " + fileName);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("File không hợp lệ: " + fileName, e);
        }
    }
    
    @Override
    public String getUserAvatarUrl(String userId) {
        return mediaRepository.findByEntityTypeAndEntityIdAndMediaType(
                EntityType.USER, userId, MediaType.PROFILE_AVATAR)
            .map(Media::getFullUrl)
            .orElse("/images/default-avatar.png");
    }
    
    @Override
    public String getVocabAudioUrl(Integer vocabId) {
        return mediaRepository.findByEntityTypeAndEntityIdAndMediaType(
                EntityType.VOCABULARY, String.valueOf(vocabId), MediaType.VOCAB_AUDIO)
            .map(Media::getFullUrl)
            .orElse(null);
    }
    
    @Override
    public boolean checkUserStorageQuota(String userId, long fileSize) {
        Long totalUsed = getTotalStorageUsed(userId);
        return (totalUsed + fileSize) <= userStorageQuota;
    }
    
    @Override
    public Long getTotalStorageUsed(String userId) {
        return mediaRepository.getTotalFileSizeByUser(userId);
    }
    
    @Override
    public MediaResponseDto convertToDto(Media media) {
        return MediaResponseDto.builder()
            .mediaId(media.getMediaId())
            .mediaType(media.getMediaType())
            .fileName(media.getFileName())
            .fullUrl(media.getFullUrl())
            .fileSize(media.getFileSize())
            .formattedFileSize(media.getFormattedFileSize())
            .mimeType(media.getMimeType())
            .entityType(media.getEntityType())
            .entityId(media.getEntityId())
            .isPrimary(media.getIsPrimary())
            .description(media.getDescription())
            .metadata(media.getMetadata())
            .uploaderId(media.getUploader().getId())  // No parsing needed - both are String (UUID)
            .uploaderName(media.getUploader().getUsername())
            .createdAt(media.getCreatedAt())
            .updatedAt(media.getUpdatedAt())
            .build();
    }
    
    // ============ Private Helper Methods ============
    
    private void validateFile(MultipartFile file, MediaType mediaType) {
        if (file.isEmpty()) {
            throw new RuntimeException("File không được để trống");
        }
        
        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("File quá lớn. Kích thước tối đa: " + (maxFileSize / 1024 / 1024) + "MB");
        }
        
        String contentType = file.getContentType();
        
        // Validate based on media type
        if (mediaType == MediaType.PROFILE_AVATAR || mediaType == MediaType.VOCAB_IMAGE) {
            if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
                throw new RuntimeException("Chỉ chấp nhận file ảnh: JPG, PNG, GIF, WEBP");
            }
        } else if (mediaType == MediaType.VOCAB_AUDIO) {
            if (contentType == null || !ALLOWED_AUDIO_TYPES.contains(contentType)) {
                throw new RuntimeException("Chỉ chấp nhận file audio: MP3, WAV, OGG");
            }
        }
    }
    
    private String saveFile(MultipartFile file, MediaType mediaType) throws IOException {
        // Create upload directory if not exists
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Generate unique filename
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uniqueFilename = mediaType.name().toLowerCase() + "_" + timestamp + "_" + UUID.randomUUID().toString().substring(0, 8) + fileExtension;
        
        // Save file
        Path targetLocation = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        
        return uniqueFilename;
    }
    
    private void deleteFileFromDisk(String filePath) throws IOException {
        Path file = Paths.get(uploadDir).resolve(filePath).normalize();
        Files.deleteIfExists(file);
    }
    
    private void deleteOldMediaForEntity(EntityType entityType, String entityId, MediaType mediaType) throws IOException {
        List<Media> oldMediaList = mediaRepository.findByMediaTypeAndEntityTypeAndEntityId(
            mediaType, entityType, entityId
        );
        
        for (Media oldMedia : oldMediaList) {
            deleteFileFromDisk(oldMedia.getFilePath());
            mediaRepository.delete(oldMedia);
        }
    }
    
    @Override
    public int cleanupOrphanedFiles() throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        
        if (!Files.exists(uploadPath)) {
            log.warn("Upload directory does not exist: {}", uploadDir);
            return 0;
        }
        
        // Get all files from uploads directory
        List<String> filesOnDisk = Files.list(uploadPath)
            .filter(Files::isRegularFile)
            .map(path -> path.getFileName().toString())
            .toList();
        
        // Get all file paths from database
        List<String> filesInDatabase = mediaRepository.findAll().stream()
            .map(Media::getFilePath)
            .toList();
        
        // Find orphaned files (in disk but not in database)
        List<String> orphanedFiles = filesOnDisk.stream()
            .filter(file -> !filesInDatabase.contains(file))
            .toList();
        
        // Delete orphaned files
        int deletedCount = 0;
        for (String orphanedFile : orphanedFiles) {
            try {
                deleteFileFromDisk(orphanedFile);
                deletedCount++;
                log.info("Deleted orphaned file: {}", orphanedFile);
            } catch (IOException e) {
                log.error("Failed to delete orphaned file: {}", orphanedFile, e);
            }
        }
        
        log.info("Cleanup completed: {} orphaned files deleted out of {} total files on disk", 
            deletedCount, filesOnDisk.size());
        
        return deletedCount;
    }
}
