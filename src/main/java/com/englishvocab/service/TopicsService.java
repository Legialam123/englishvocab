package com.englishvocab.service;

import com.englishvocab.entity.Topics;
import com.englishvocab.repository.TopicsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TopicsService {
    
    private final TopicsRepository topicsRepository;
    
    /**
     * Lấy tất cả chủ đề
     */
    public List<Topics> findAll() {
        return topicsRepository.findAll();
    }
    
    /**
     * Lấy chủ đề theo ID
     */
    public Optional<Topics> findById(Integer id) {
        return topicsRepository.findById(id);
    }
    
    /**
     * Lấy chủ đề theo ID hoặc throw exception
     */
    public Topics findByIdOrThrow(Integer id) {
        return topicsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chủ đề với ID: " + id));
    }
    
    /**
     * Lấy chủ đề active
     */
    public List<Topics> findActiveTopics() {
        return topicsRepository.findByStatusOrderByName(Topics.Status.ACTIVE);
    }
    
    /**
     * Lấy chủ đề với phân trang
     */
    public Page<Topics> findAllWithPagination(Topics.Status status, Pageable pageable) {
        if (status != null) {
            return topicsRepository.findByStatusOrderByName(status, pageable);
        }
        return topicsRepository.findAll(pageable);
    }
    
    /**
     * Tìm kiếm chủ đề theo tên
     */
    public List<Topics> searchByName(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        return topicsRepository.findByNameContainingIgnoreCase(keyword.trim());
    }
    
    /**
     * Tạo chủ đề mới
     */
    public Topics create(Topics topic) {
        log.info("Tạo chủ đề mới: {}", topic.getName());
        
        // Validate business rules
        validateTopic(topic);
        
        // Check duplicate name
        if (topicsRepository.existsByName(topic.getName())) {
            throw new RuntimeException("Tên chủ đề đã tồn tại: " + topic.getName());
        }
        
        // Set default status if not set
        if (topic.getStatus() == null) {
            topic.setStatus(Topics.Status.ACTIVE);
        }
        
        Topics saved = topicsRepository.save(topic);
        log.info("Đã tạo chủ đề với ID: {}", saved.getTopicId());
        return saved;
    }
    
    /**
     * Cập nhật chủ đề
     */
    public Topics update(Integer id, Topics updatedTopic) {
        log.info("Cập nhật chủ đề ID: {}", id);
        
        Topics existing = findByIdOrThrow(id);
        
        // Validate business rules
        validateTopic(updatedTopic);
        
        // Check duplicate name (exclude current record)
        topicsRepository.findByName(updatedTopic.getName())
                .ifPresent(topic -> {
                    if (!topic.getTopicId().equals(id)) {
                        throw new RuntimeException("Tên chủ đề đã tồn tại: " + updatedTopic.getName());
                    }
                });
        
        // Update fields
        existing.setName(updatedTopic.getName());
        existing.setDescription(updatedTopic.getDescription());
        existing.setStatus(updatedTopic.getStatus());
        
        Topics saved = topicsRepository.save(existing);
        log.info("Đã cập nhật chủ đề: {}", saved.getName());
        return saved;
    }
    
    /**
     * Xóa chủ đề (soft delete)
     */
    public void delete(Integer id) {
        log.info("Xóa chủ đề ID: {}", id);
        
        Topics topic = findByIdOrThrow(id);
        
        // Check if topic has vocabulary relationships
        try {
            long vocabCount = topicsRepository.countVocabularyByTopic(id);
            if (vocabCount > 0) {
                // Soft delete - set status to INACTIVE
                topic.setStatus(Topics.Status.INACTIVE);
                topicsRepository.save(topic);
                log.info("Đã vô hiệu hóa chủ đề có {} từ vựng: {}", vocabCount, topic.getName());
            } else {
                // Hard delete if no vocabulary
                topicsRepository.delete(topic);
                log.info("Đã xóa chủ đề: {}", topic.getName());
            }
        } catch (Exception e) {
            // If counting fails, do soft delete to be safe
            log.warn("Lỗi khi đếm từ vựng, thực hiện soft delete: {}", e.getMessage());
            topic.setStatus(Topics.Status.INACTIVE);
            topicsRepository.save(topic);
        }
    }
    
    /**
     * Toggle status chủ đề
     */
    public Topics toggleStatus(Integer id) {
        Topics topic = findByIdOrThrow(id);
        
        Topics.Status newStatus = topic.getStatus() == Topics.Status.ACTIVE 
            ? Topics.Status.INACTIVE 
            : Topics.Status.ACTIVE;
            
        topic.setStatus(newStatus);
        Topics saved = topicsRepository.save(topic);
        
        log.info("Đã chuyển trạng thái chủ đề '{}' thành: {}", saved.getName(), newStatus);
        return saved;
    }
    
    /**
     * Thống kê chủ đề
     */
    public TopicStats getStatistics() {
        long total = topicsRepository.count();
        long active = topicsRepository.countByStatus(Topics.Status.ACTIVE);
        long inactive = topicsRepository.countByStatus(Topics.Status.INACTIVE);
        
        return TopicStats.builder()
                .total(total)
                .active(active)
                .inactive(inactive)
                .build();
    }
    
    /**
     * Lấy chủ đề có nhiều từ vựng nhất
     */
    public List<TopicWithVocabCount> getTopicsWithVocabCount(int limit) {
        try {
            return topicsRepository.findTopicsWithVocabCount()
                    .stream()
                    .limit(limit)
                    .map(result -> TopicWithVocabCount.builder()
                            .topic((Topics) result[0])
                            .vocabCount((Long) result[1])
                            .build())
                    .toList();
        } catch (Exception e) {
            log.warn("Lỗi khi lấy top topics: {}", e.getMessage());
            // Return empty list if query fails
            return List.of();
        }
    }
    
    /**
     * Validate topic
     */
    private void validateTopic(Topics topic) {
        if (topic.getName() == null || topic.getName().trim().isEmpty()) {
            throw new RuntimeException("Tên chủ đề không được để trống");
        }
        
        if (topic.getName().length() > 100) {
            throw new RuntimeException("Tên chủ đề không được vượt quá 100 ký tự");
        }
        
        if (topic.getDescription() != null && topic.getDescription().length() > 255) {
            throw new RuntimeException("Mô tả không được vượt quá 255 ký tự");
        }
    }
    
    /**
     * Topic Statistics DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class TopicStats {
        private long total;
        private long active;
        private long inactive;
    }
    
    /**
     * Topic with Vocabulary Count DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class TopicWithVocabCount {
        private Topics topic;
        private Long vocabCount;
    }
}