package com.englishvocab.service;

import com.englishvocab.entity.User;
import com.englishvocab.entity.UserCustomVocab;
import com.englishvocab.repository.UserCustomVocabRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service quản lý từ vựng cá nhân của user
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserCustomVocabService {
    
    private final UserCustomVocabRepository userCustomVocabRepository;
    
    /**
     * Lấy tất cả từ vựng cá nhân của user
     */
    @Transactional(readOnly = true)
    public List<UserCustomVocab> findByUser(User user) {
        return userCustomVocabRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    /**
     * Lấy từ vựng cá nhân của user với phân trang
     */
    @Transactional(readOnly = true)
    public Page<UserCustomVocab> findByUser(User user, Pageable pageable) {
        return userCustomVocabRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }
    
    /**
     * Tìm từ vựng cá nhân theo ID
     */
    @Transactional(readOnly = true)
    public Optional<UserCustomVocab> findById(Integer id) {
        return userCustomVocabRepository.findById(id);
    }
    
    /**
     * Tìm từ vựng cá nhân theo ID hoặc throw exception
     */
    @Transactional(readOnly = true)
    public UserCustomVocab findByIdOrThrow(Integer id) {
        return userCustomVocabRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy từ vựng cá nhân với ID: " + id));
    }
    
    /**
     * Kiểm tra từ vựng đã tồn tại cho user chưa
     */
    @Transactional(readOnly = true)
    public boolean existsByUserAndWord(User user, String word) {
        return userCustomVocabRepository.existsByUserAndNameIgnoreCase(user, word);
    }
    
    /**
     * Tìm từ vựng cá nhân theo tên
     */
    @Transactional(readOnly = true)
    public List<UserCustomVocab> findByUserAndWordContaining(User user, String keyword) {
        return userCustomVocabRepository.findByUserAndNameContainingIgnoreCase(user, keyword);
    }
    
    /**
     * Đếm số từ vựng cá nhân của user
     */
    @Transactional(readOnly = true)
    public long countByUser(User user) {
        return userCustomVocabRepository.countByUser(user);
    }
    
    /**
     * Lưu từ vựng cá nhân mới
     */
    public UserCustomVocab save(UserCustomVocab customVocab) {
        try {
            // Validate required fields
            if (customVocab.getUser() == null) {
                throw new IllegalArgumentException("User không được null");
            }
            if (customVocab.getName() == null || customVocab.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Tên từ vựng không được để trống");
            }
            
            // Check for duplicates
            if (existsByUserAndWord(customVocab.getUser(), customVocab.getName())) {
                throw new RuntimeException("Từ vựng '" + customVocab.getName() + "' đã tồn tại");
            }
            
            // Clean data
            customVocab.setName(customVocab.getName().trim().toLowerCase());
            if (customVocab.getIpa() != null) {
                customVocab.setIpa(customVocab.getIpa().trim());
            }
            if (customVocab.getPos() != null) {
                customVocab.setPos(customVocab.getPos().trim().toLowerCase());
            }
            if (customVocab.getMeaningVi() != null) {
                customVocab.setMeaningVi(customVocab.getMeaningVi().trim());
            }
            
            UserCustomVocab saved = userCustomVocabRepository.save(customVocab);
            log.info("User {} created custom vocabulary: {}", 
                    customVocab.getUser().getUsername(), customVocab.getName());
            
            return saved;
            
        } catch (Exception e) {
            log.error("Error saving custom vocabulary for user: {}", 
                    customVocab.getUser().getUsername(), e);
            throw new RuntimeException("Không thể lưu từ vựng: " + e.getMessage());
        }
    }
    
    /**
     * Cập nhật từ vựng cá nhân
     */
    public UserCustomVocab update(Integer id, UserCustomVocab updatedVocab) {
        try {
            UserCustomVocab existingVocab = findByIdOrThrow(id);
            
            // Validate ownership
            if (!existingVocab.getUser().getId().equals(updatedVocab.getUser().getId())) {
                throw new RuntimeException("Bạn không có quyền cập nhật từ vựng này");
            }
            
            // Check for name conflicts (excluding current record)
            if (!existingVocab.getName().equalsIgnoreCase(updatedVocab.getName())) {
                if (existsByUserAndWord(updatedVocab.getUser(), updatedVocab.getName())) {
                    throw new RuntimeException("Từ vựng '" + updatedVocab.getName() + "' đã tồn tại");
                }
            }
            
            // Update fields
            existingVocab.setName(updatedVocab.getName().trim().toLowerCase());
            existingVocab.setIpa(updatedVocab.getIpa() != null ? updatedVocab.getIpa().trim() : null);
            existingVocab.setPos(updatedVocab.getPos() != null ? updatedVocab.getPos().trim().toLowerCase() : null);
            existingVocab.setMeaningVi(updatedVocab.getMeaningVi() != null ? updatedVocab.getMeaningVi().trim() : null);
            
            UserCustomVocab updated = userCustomVocabRepository.save(existingVocab);
            log.info("User {} updated custom vocabulary: {} (ID: {})", 
                    updatedVocab.getUser().getUsername(), updatedVocab.getName(), id);
            
            return updated;
            
        } catch (Exception e) {
            log.error("Error updating custom vocabulary ID: {}", id, e);
            throw new RuntimeException("Không thể cập nhật từ vựng: " + e.getMessage());
        }
    }
    
    /**
     * Xóa từ vựng cá nhân
     */
    public void delete(Integer id) {
        try {
            UserCustomVocab vocab = findByIdOrThrow(id);
            
            // TODO: Check if vocab is being used in any learning progress
            // We might want to keep the record but mark as deleted instead of hard delete
            
            userCustomVocabRepository.delete(vocab);
            log.info("User {} deleted custom vocabulary: {} (ID: {})", 
                    vocab.getUser().getUsername(), vocab.getName(), id);
            
        } catch (Exception e) {
            log.error("Error deleting custom vocabulary ID: {}", id, e);
            throw new RuntimeException("Không thể xóa từ vựng: " + e.getMessage());
        }
    }
    
    /**
     * Tìm kiếm từ vựng cá nhân của user
     */
    @Transactional(readOnly = true)
    public List<UserCustomVocab> searchUserVocabulary(User user, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findByUser(user);
        }
        
        String searchTerm = keyword.trim().toLowerCase();
        return userCustomVocabRepository.findByUserAndSearchTerm(user, searchTerm);
    }
    
    /**
     * Lấy từ vựng mới nhất của user
     */
    @Transactional(readOnly = true)
    public List<UserCustomVocab> findRecentByUser(User user, int limit) {
        return userCustomVocabRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .limit(limit)
                .toList();
    }
    
    /**
     * Thống kê từ vựng cá nhân của user
     */
    @Transactional(readOnly = true)
    public UserCustomVocabStats getStatistics(User user) {
        long total = countByUser(user);
        
        // Count by parts of speech
        long nouns = userCustomVocabRepository.countByUserAndPos(user, "noun");
        long verbs = userCustomVocabRepository.countByUserAndPos(user, "verb");
        long adjectives = userCustomVocabRepository.countByUserAndPos(user, "adjective");
        long others = total - nouns - verbs - adjectives;
        
        return UserCustomVocabStats.builder()
                .total(total)
                .nouns(nouns)
                .verbs(verbs)
                .adjectives(adjectives)
                .others(others)
                .build();
    }
    
    /**
     * Stats DTO cho từ vựng cá nhân
     */
    @lombok.Data
    @lombok.Builder
    public static class UserCustomVocabStats {
        private long total;
        private long nouns;
        private long verbs;
        private long adjectives;
        private long others;
    }
}
