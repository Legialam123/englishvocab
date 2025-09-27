package com.englishvocab.service;

import com.englishvocab.entity.Dictionary;
import com.englishvocab.repository.DictionaryRepository;
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
public class DictionaryService {
    
    private final DictionaryRepository dictionaryRepository;
    
    /**
     * Lấy tất cả từ điển
     */
    public List<Dictionary> findAll() {
        return dictionaryRepository.findAll();
    }
    
    /**
     * Lấy từ điển theo ID
     */
    public Optional<Dictionary> findById(Integer id) {
        return dictionaryRepository.findById(id);
    }
    
    /**
     * Lấy từ điển theo ID hoặc throw exception
     */
    public Dictionary findByIdOrThrow(Integer id) {
        return dictionaryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy từ điển với ID: " + id));
    }
    
    /**
     * Lấy từ điển active
     */
    public List<Dictionary> findActiveDictionaries() {
        return dictionaryRepository.findByStatusOrderByName(Dictionary.Status.ACTIVE);
    }
    
    /**
     * Lấy từ điển với phân trang
     */
    public Page<Dictionary> findAllWithPagination(Dictionary.Status status, Pageable pageable) {
        if (status != null) {
            return dictionaryRepository.findByStatusOrderByName(status, pageable);
        }
        return dictionaryRepository.findAll(pageable);
    }
    
    /**
     * Tìm kiếm từ điển theo tên
     */
    public List<Dictionary> searchByName(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        return dictionaryRepository.findByNameContainingIgnoreCase(keyword.trim());
    }
    
    /**
     * Tạo từ điển mới
     */
    public Dictionary create(Dictionary dictionary) {
        log.info("Tạo từ điển mới: {}", dictionary.getName());
        
        // Validate business rules
        validateDictionary(dictionary);
        
        // Check duplicate name
        if (dictionaryRepository.existsByName(dictionary.getName())) {
            throw new RuntimeException("Tên từ điển đã tồn tại: " + dictionary.getName());
        }
        
        // Check duplicate code
        if (dictionary.getCode() != null && dictionaryRepository.existsByCode(dictionary.getCode())) {
            throw new RuntimeException("Mã từ điển đã tồn tại: " + dictionary.getCode());
        }
        
        Dictionary saved = dictionaryRepository.save(dictionary);
        log.info("Đã tạo từ điển với ID: {}", saved.getDictionaryId());
        return saved;
    }
    
    /**
     * Cập nhật từ điển
     */
    public Dictionary update(Integer id, Dictionary updatedDictionary) {
        log.info("Cập nhật từ điển ID: {}", id);
        
        Dictionary existing = findByIdOrThrow(id);
        
        // Validate business rules
        validateDictionary(updatedDictionary);
        
        // Check duplicate name (exclude current record)
        dictionaryRepository.findByName(updatedDictionary.getName())
                .ifPresent(dict -> {
                    if (!dict.getDictionaryId().equals(id)) {
                        throw new RuntimeException("Tên từ điển đã tồn tại: " + updatedDictionary.getName());
                    }
                });
        
        // Check duplicate code (exclude current record)
        if (updatedDictionary.getCode() != null) {
            dictionaryRepository.findByCode(updatedDictionary.getCode())
                    .ifPresent(dict -> {
                        if (!dict.getDictionaryId().equals(id)) {
                            throw new RuntimeException("Mã từ điển đã tồn tại: " + updatedDictionary.getCode());
                        }
                    });
        }
        
        // Update fields
        existing.setName(updatedDictionary.getName());
        existing.setCode(updatedDictionary.getCode());
        existing.setPublisher(updatedDictionary.getPublisher());
        existing.setStatus(updatedDictionary.getStatus());
        existing.setDescription(updatedDictionary.getDescription());
        
        Dictionary saved = dictionaryRepository.save(existing);
        log.info("Đã cập nhật từ điển: {}", saved.getName());
        return saved;
    }
    
    /**
     * Xóa từ điển (soft delete)
     */
    public void delete(Integer id) {
        log.info("Xóa từ điển ID: {}", id);
        
        Dictionary dictionary = findByIdOrThrow(id);
        
        // Check if dictionary has vocabulary
        long vocabCount = dictionaryRepository.countByStatus(Dictionary.Status.ACTIVE);
        if (vocabCount > 0) {
            // Soft delete - set status to ARCHIVED
            dictionary.setStatus(Dictionary.Status.ARCHIVED);
            dictionaryRepository.save(dictionary);
            log.info("Đã archive từ điển: {}", dictionary.getName());
        } else {
            // Hard delete if no vocabulary
            dictionaryRepository.delete(dictionary);
            log.info("Đã xóa từ điển: {}", dictionary.getName());
        }
    }
    
    /**
     * Thống kê từ điển
     */
    public DictionaryStats getStatistics() {
        long total = dictionaryRepository.count();
        long active = dictionaryRepository.countByStatus(Dictionary.Status.ACTIVE);
        long inactive = dictionaryRepository.countByStatus(Dictionary.Status.INACTIVE);
        long archived = dictionaryRepository.countByStatus(Dictionary.Status.ARCHIVED);

        return DictionaryStats.builder()
                .total(total)
                .active(active)
                .inactive(inactive)
                .archived(archived)
                .build();
    }
    
    /**
     * Validate dictionary
     */
    private void validateDictionary(Dictionary dictionary) {
        if (dictionary.getName() == null || dictionary.getName().trim().isEmpty()) {
            throw new RuntimeException("Tên từ điển không được để trống");
        }
        
        if (dictionary.getName().length() > 100) {
            throw new RuntimeException("Tên từ điển không được vượt quá 100 ký tự");
        }
        
        if (dictionary.getCode() != null && dictionary.getCode().length() > 50) {
            throw new RuntimeException("Mã từ điển không được vượt quá 50 ký tự");
        }
        
        if (dictionary.getPublisher() != null && dictionary.getPublisher().length() > 100) {
            throw new RuntimeException("Nhà xuất bản không được vượt quá 100 ký tự");
        }
        
        if (dictionary.getDescription() != null && dictionary.getDescription().length() > 150) {
            throw new RuntimeException("Mô tả không được vượt quá 150 ký tự");
        }
    }
    
    /**
     * Dictionary Statistics DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class DictionaryStats {
        private long total;
        private long active;
        private long inactive;
        private long archived;
    }
}
