package com.englishvocab.repository;

import com.englishvocab.entity.Media;
import com.englishvocab.entity.Media.EntityType;
import com.englishvocab.entity.Media.MediaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {
    
    // Tìm media theo entity
    List<Media> findByEntityTypeAndEntityId(EntityType entityType, String entityId);
    
    // Tìm primary media của entity
    Optional<Media> findByEntityTypeAndEntityIdAndIsPrimaryTrue(
        EntityType entityType, String entityId
    );
    
    // Tìm media theo type và entity
    List<Media> findByMediaTypeAndEntityTypeAndEntityId(
        MediaType mediaType, EntityType entityType, String entityId
    );
    
    // Tìm tất cả media của user (người upload)
    List<Media> findByUploader_Id(String userId);
    
    // Tìm avatar của user
    Optional<Media> findByEntityTypeAndEntityIdAndMediaType(
        EntityType entityType, String entityId, MediaType mediaType
    );
    
    // Đếm số lượng media theo user
    long countByUploader_Id(String userId);
    
    // Tính tổng dung lượng file của user (by user ID)
    @Query("SELECT COALESCE(SUM(m.fileSize), 0) FROM Media m WHERE m.uploader.id = :userId")
    Long getTotalFileSizeByUser(@Param("userId") String userId);

    // Set tất cả media của entity thành not primary
    @Modifying
    @Query("UPDATE Media m SET m.isPrimary = false " +
           "WHERE m.entityType = :entityType AND m.entityId = :entityId")
    void unsetAllPrimaryForEntity(
        @Param("entityType") EntityType entityType,
        @Param("entityId") String entityId
    );
    
    // Xóa media cũ khi upload mới (cho avatar chẳng hạn)
    @Modifying
    @Query("DELETE FROM Media m WHERE m.entityType = :entityType " +
           "AND m.entityId = :entityId AND m.mediaType = :mediaType")
    void deleteByEntityAndType(
        @Param("entityType") EntityType entityType,
        @Param("entityId") String entityId,
        @Param("mediaType") MediaType mediaType
    );
    
    // Tìm media theo file path
    Optional<Media> findByFilePath(String filePath);
    
    // Check xem entity đã có media chưa
    boolean existsByEntityTypeAndEntityIdAndMediaType(
        EntityType entityType, String entityId, MediaType mediaType
    );
}
