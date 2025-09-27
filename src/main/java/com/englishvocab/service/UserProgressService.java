package com.englishvocab.service;

import com.englishvocab.entity.User;
import com.englishvocab.entity.Vocab;
import com.englishvocab.entity.UserVocabProgress;
import com.englishvocab.repository.UserVocabProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service quản lý progress học tập của user với SRS cơ bản
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserProgressService {
    
    private final UserVocabProgressRepository progressRepository;
    
    // SRS intervals in days for each box (Simple Leitner System)
    private static final int[] SRS_INTERVALS = {1, 3, 7, 14, 30}; // Box 1-5
    
    /**
     * Lấy progress của user với phân trang
     */
    @Transactional(readOnly = true)
    public Page<UserVocabProgress> findUserProgress(User user, Pageable pageable) {
        return progressRepository.findByUser(user).stream()
                .collect(java.util.stream.Collectors.collectingAndThen(
                    java.util.stream.Collectors.toList(),
                    list -> new org.springframework.data.domain.PageImpl<>(
                        list.stream().skip(pageable.getOffset()).limit(pageable.getPageSize()).toList(),
                        pageable,
                        list.size()
                    )
                ));
    }
    
    /**
     * Lấy tất cả progress của user
     */
    @Transactional(readOnly = true)
    public List<UserVocabProgress> findUserProgress(User user) {
        return progressRepository.findByUser(user);
    }
    
    /**
     * Lấy progress của user với từ vựng cụ thể
     */
    @Transactional(readOnly = true)
    public UserVocabProgress findUserProgress(User user, Vocab vocab) {
        return progressRepository.findByUserAndVocab(user, vocab).orElse(null);
    }
    
    /**
     * Đếm từ đã học của user
     */
    @Transactional(readOnly = true)
    public long countLearnedWords(User user) {
        return progressRepository.countByUser(user);
    }
    
    /**
     * Đếm từ cần review hôm nay
     */
    @Transactional(readOnly = true)
    public long countWordsForReview(User user) {
        return progressRepository.countDueForReview(user, LocalDateTime.now());
    }
    
    /**
     * Lấy từ cần review hôm nay
     */
    @Transactional(readOnly = true)
    public List<UserVocabProgress> getWordsForReview(User user) {
        return progressRepository.findDueForReview(user, LocalDateTime.now());
    }
    
    /**
     * Lấy từ cần review với limit
     */
    @Transactional(readOnly = true)
    public List<UserVocabProgress> getWordsForReview(User user, int limit) {
        return progressRepository.findDueForReview(user, LocalDateTime.now())
                .stream()
                .limit(limit)
                .toList();
    }
    
    /**
     * Bắt đầu học từ mới
     */
    public UserVocabProgress startLearning(User user, Vocab vocab) {
        try {
            // Check if progress already exists
            Optional<UserVocabProgress> existingProgress = progressRepository.findByUserAndVocab(user, vocab);
            if (existingProgress.isPresent()) {
                log.info("User {} already has progress for vocab: {}", user.getUsername(), vocab.getWord());
                return existingProgress.get();
            }
            
            // Create new progress
            UserVocabProgress progress = UserVocabProgress.builder()
                    .user(user)
                    .vocab(vocab)
                    .box(1) // Start at box 1
                    .status(UserVocabProgress.Status.LEARNING)
                    .firstLearned(LocalDateTime.now())
                    .lastReviewed(LocalDateTime.now())
                    .nextReviewAt(calculateNextReview(1)) // Next review in 1 day
                    .streak(0)
                    .wrongCount(0)
                    .build();
            
            UserVocabProgress saved = progressRepository.save(progress);
            log.info("User {} started learning vocab: {} (Box 1)", user.getUsername(), vocab.getWord());
            
            return saved;
            
        } catch (Exception e) {
            log.error("Error starting learning for user: {} vocab: {}", user.getUsername(), vocab.getWord(), e);
            throw new RuntimeException("Không thể bắt đầu học từ này: " + e.getMessage());
        }
    }
    
    /**
     * Xử lý câu trả lời đúng
     */
    public UserVocabProgress processCorrectAnswer(User user, Vocab vocab) {
        try {
            UserVocabProgress progress = progressRepository.findByUserAndVocab(user, vocab)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy progress cho từ này"));
            
            // Update progress
            progress.setLastReviewed(LocalDateTime.now());
            progress.setStreak(progress.getStreak() + 1);
            
            // Move to next box (max 5)
            int currentBox = progress.getBox();
            if (currentBox < 5) {
                progress.setBox(currentBox + 1);
            }
            
            // Calculate next review
            progress.setNextReviewAt(calculateNextReview(progress.getBox()));
            
            // Check for mastery (Box 5 with streak >= 3)
            if (progress.getBox() == 5 && progress.getStreak() >= 3) {
                progress.setStatus(UserVocabProgress.Status.MASTERED);
                log.info("User {} mastered vocab: {}", user.getUsername(), vocab.getWord());
            }
            
            UserVocabProgress updated = progressRepository.save(progress);
            log.info("User {} answered correctly: {} (Box {} -> {})", 
                    user.getUsername(), vocab.getWord(), currentBox, progress.getBox());
            
            return updated;
            
        } catch (Exception e) {
            log.error("Error processing correct answer for user: {} vocab: {}", 
                    user.getUsername(), vocab.getWord(), e);
            throw new RuntimeException("Không thể xử lý câu trả lời: " + e.getMessage());
        }
    }
    
    /**
     * Xử lý câu trả lời sai
     */
    public UserVocabProgress processWrongAnswer(User user, Vocab vocab) {
        try {
            UserVocabProgress progress = progressRepository.findByUserAndVocab(user, vocab)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy progress cho từ này"));
            
            // Update progress
            progress.setLastReviewed(LocalDateTime.now());
            progress.setStreak(0); // Reset streak
            progress.setWrongCount(progress.getWrongCount() + 1);
            
            // Move back to box 1 (restart SRS)
            int currentBox = progress.getBox();
            progress.setBox(1);
            
            // Next review in 1 day
            progress.setNextReviewAt(calculateNextReview(1));
            
            // Set status back to learning
            if (progress.getStatus() == UserVocabProgress.Status.MASTERED) {
                progress.setStatus(UserVocabProgress.Status.LEARNING);
            }
            
            UserVocabProgress updated = progressRepository.save(progress);
            log.info("User {} answered incorrectly: {} (Box {} -> 1)", 
                    user.getUsername(), vocab.getWord(), currentBox);
            
            return updated;
            
        } catch (Exception e) {
            log.error("Error processing wrong answer for user: {} vocab: {}", 
                    user.getUsername(), vocab.getWord(), e);
            throw new RuntimeException("Không thể xử lý câu trả lời: " + e.getMessage());
        }
    }
    
    /**
     * Lấy thống kê học tập của user
     */
    @Transactional(readOnly = true)
    public LearningStatistics getLearningStatistics(User user) {
        long totalWords = countLearnedWords(user);
        long wordsToReview = countWordsForReview(user);
        long masteredWords = progressRepository.countByUserAndStatus(user, UserVocabProgress.Status.MASTERED);
        long learningWords = progressRepository.countByUserAndStatus(user, UserVocabProgress.Status.LEARNING);
        
        // Calculate accuracy
        List<UserVocabProgress> allProgress = findUserProgress(user);
        double accuracy = calculateAccuracy(allProgress);
        
        // Get box statistics
        List<Object[]> boxStats = progressRepository.getBoxStatistics(user);
        
        // Calculate current streak
        int currentStreak = calculateCurrentStreak(user);
        
        return LearningStatistics.builder()
                .totalWords(totalWords)
                .wordsToReview(wordsToReview)
                .masteredWords(masteredWords)
                .learningWords(learningWords)
                .accuracy(accuracy)
                .currentStreak(currentStreak)
                .boxStatistics(boxStats)
                .build();
    }
    
    /**
     * Lấy từ khó nhớ của user
     */
    @Transactional(readOnly = true)
    public List<UserVocabProgress> getDifficultWords(User user, int minWrongCount, int limit) {
        return progressRepository.findDifficultWords(user, minWrongCount)
                .stream()
                .limit(limit)
                .toList();
    }
    
    /**
     * Lấy từ đã mastered
     */
    @Transactional(readOnly = true)
    public List<UserVocabProgress> getMasteredWords(User user) {
        return progressRepository.findMasteredWords(user);
    }
    
    /**
     * Reset progress của một từ
     */
    public UserVocabProgress resetProgress(User user, Vocab vocab) {
        try {
            UserVocabProgress progress = progressRepository.findByUserAndVocab(user, vocab)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy progress cho từ này"));
            
            // Reset to initial state
            progress.setBox(1);
            progress.setStatus(UserVocabProgress.Status.LEARNING);
            progress.setStreak(0);
            progress.setWrongCount(0);
            progress.setLastReviewed(LocalDateTime.now());
            progress.setNextReviewAt(calculateNextReview(1));
            
            UserVocabProgress updated = progressRepository.save(progress);
            log.info("User {} reset progress for vocab: {}", user.getUsername(), vocab.getWord());
            
            return updated;
            
        } catch (Exception e) {
            log.error("Error resetting progress for user: {} vocab: {}", 
                    user.getUsername(), vocab.getWord(), e);
            throw new RuntimeException("Không thể reset progress: " + e.getMessage());
        }
    }
    
    // ===== PRIVATE HELPER METHODS =====
    
    /**
     * Tính toán thời gian review tiếp theo dựa trên SRS
     */
    private LocalDateTime calculateNextReview(int box) {
        if (box < 1 || box > 5) {
            box = 1; // Default to box 1
        }
        int daysToAdd = SRS_INTERVALS[box - 1];
        return LocalDateTime.now().plusDays(daysToAdd);
    }
    
    /**
     * Tính toán độ chính xác
     */
    private double calculateAccuracy(List<UserVocabProgress> progressList) {
        if (progressList.isEmpty()) {
            return 0.0;
        }
        
        int totalAttempts = 0;
        int totalWrongAnswers = 0;
        
        for (UserVocabProgress progress : progressList) {
            // Estimate total attempts from streak and wrong count
            int attempts = progress.getStreak() + progress.getWrongCount();
            if (attempts > 0) {
                totalAttempts += attempts;
                totalWrongAnswers += progress.getWrongCount();
            }
        }
        
        if (totalAttempts == 0) {
            return 0.0;
        }
        
        return ((double) (totalAttempts - totalWrongAnswers) / totalAttempts) * 100;
    }
    
    /**
     * Tính toán learning streak hiện tại
     */
    private int calculateCurrentStreak(User user) {
        // Get recent activity (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<UserVocabProgress> recentActivity = progressRepository.findRecentActivity(user, thirtyDaysAgo);
        
        // Simple streak calculation - consecutive days with activity
        // This is a basic implementation, can be improved
        return Math.min(recentActivity.size() / 5, 30); // Rough estimate
    }
    
    // ===== DATA TRANSFER OBJECTS =====
    
    /**
     * DTO cho thống kê học tập
     */
    @lombok.Data
    @lombok.Builder
    public static class LearningStatistics {
        private long totalWords;
        private long wordsToReview;
        private long masteredWords;
        private long learningWords;
        private double accuracy;
        private int currentStreak;
        private List<Object[]> boxStatistics;
    }
}
