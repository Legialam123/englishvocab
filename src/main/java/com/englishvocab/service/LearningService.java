package com.englishvocab.service;

import com.englishvocab.entity.Dictionary;
import com.englishvocab.entity.User;
import com.englishvocab.entity.UserVocabProgress;
import com.englishvocab.entity.Vocab;
import com.englishvocab.repository.DictionaryRepository;
import com.englishvocab.repository.UserRepository;
import com.englishvocab.repository.UserVocabProgressRepository;
import com.englishvocab.repository.VocabRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service xử lý logic học từ vựng
 * 
 * Chức năng chính:
 * 1. Tạo learning sessions
 * 2. Quản lý flashcard sessions
 * 3. Cập nhật progress học tập
 * 4. Tính toán statistics
 * 
 * @author EnglishVocab Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LearningService {

    private final UserRepository userRepository;
    private final VocabRepository vocabRepository;
    private final UserVocabProgressRepository userVocabProgressRepository;
    private final DictionaryRepository dictionaryRepository;

    /**
     * 🎯 Tạo learning session mới
     */
    public String createLearningSession(
            String userId,
            Integer dictionaryId, 
            String learningMode,
            List<Integer> selectedVocabIds,
            List<Integer> topicIds,
            int sessionSize) {

        // Validate user và dictionary
        User user = userRepository.findByEmail(userId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy user: " + userId));
        
        Dictionary dictionary = dictionaryRepository.findById(dictionaryId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy dictionary với ID: " + dictionaryId));

        // Load vocabulary dựa trên learning mode
        List<Vocab> sessionVocabs = loadVocabularyForSession(
            dictionaryId, learningMode, selectedVocabIds, topicIds, sessionSize);

        if (sessionVocabs.isEmpty()) {
            throw new RuntimeException("Không tìm thấy từ vựng nào để học!");
        }

        // Create session ID
        String sessionId = generateSessionId(user.getId(), dictionaryId);
        
        // TODO: Store session in cache or database
        // For now, we'll use a simple in-memory approach
        
        log.info("Created learning session {} for user {} with {} vocabularies", 
                sessionId, userId, sessionVocabs.size());

        return sessionId;
    }

    /**
     * 📚 Load vocabulary cho session dựa trên learning mode
     */
    private List<Vocab> loadVocabularyForSession(
            Integer dictionaryId,
            String learningMode,
            List<Integer> selectedVocabIds,
            List<Integer> topicIds,
            int sessionSize) {

        Dictionary dictionary = dictionaryRepository.findById(dictionaryId)
            .orElseThrow(() -> new RuntimeException("Dictionary not found"));

        switch (learningMode.toLowerCase()) {
            case "alphabetical":
                return loadAlphabeticalVocabulary(dictionary, sessionSize);
            
            case "topics":
                return loadTopicVocabulary(dictionary, topicIds, sessionSize);
            
            case "custom":
                return loadCustomVocabulary(selectedVocabIds, sessionSize);
                
            default:
                throw new RuntimeException("Invalid learning mode: " + learningMode);
        }
    }

    /**
     * 📝 Load vocabulary theo thứ tự A-Z
     */
    private List<Vocab> loadAlphabeticalVocabulary(Dictionary dictionary, int sessionSize) {
        // Load vocabulary sorted alphabetically
        List<Vocab> allVocabs = vocabRepository.findByDictionaryOrderByWordAsc(dictionary);
        
        // Limit to session size
        return allVocabs.stream()
                .limit(sessionSize)
                .collect(Collectors.toList());
    }

    /**
     * 🏷️ Load vocabulary theo topics
     */
    private List<Vocab> loadTopicVocabulary(Dictionary dictionary, List<Integer> topicIds, int sessionSize) {
        if (topicIds == null || topicIds.isEmpty()) {
            // If no topics specified, load random vocabulary
            return vocabRepository.findByDictionary(dictionary).stream()
                    .limit(sessionSize)
                    .collect(Collectors.toList());
        }

        // TODO: Load vocabulary by topics
        // This requires joining with VocabTopics table
        List<Vocab> topicVocabs = new ArrayList<>();
        
        // For now, return random vocabulary (to be implemented)
        return vocabRepository.findByDictionary(dictionary).stream()
                .limit(sessionSize)
                .collect(Collectors.toList());
    }

    /**
     * ⚡ Load vocabulary tự chọn
     */
    private List<Vocab> loadCustomVocabulary(List<Integer> selectedVocabIds, int sessionSize) {
        if (selectedVocabIds == null || selectedVocabIds.isEmpty()) {
            throw new RuntimeException("Không có từ vựng nào được chọn!");
        }

        List<Vocab> selectedVocabs = vocabRepository.findAllById(selectedVocabIds);
        
        // Shuffle for random order
        Collections.shuffle(selectedVocabs);
        
        return selectedVocabs.stream()
                .limit(sessionSize)
                .collect(Collectors.toList());
    }

    /**
     * 🎯 Cập nhật progress khi user học xong một từ
     */
    public void updateVocabularyProgress(String userId, Integer vocabId, boolean correct) {
        User user = userRepository.findByEmail(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            
        Vocab vocab = vocabRepository.findById(vocabId)
            .orElseThrow(() -> new RuntimeException("Vocab not found: " + vocabId));

        // Find existing progress or create new
        UserVocabProgress progress = userVocabProgressRepository
            .findByUserAndVocab(user, vocab)
            .orElse(UserVocabProgress.builder()
                .user(user)
                .vocab(vocab)
                .box(1)
                .streak(0)
                .status(UserVocabProgress.Status.LEARNING)
                .wrongCount(0)
                .firstLearned(LocalDateTime.now())
                .lastReviewed(LocalDateTime.now())
                .nextReviewAt(LocalDateTime.now().plusDays(1))
                .build());

        // Update last reviewed time
        progress.setLastReviewed(LocalDateTime.now());
        
        if (correct) {
            // Increase streak and possibly move to higher box
            progress.setStreak(progress.getStreak() + 1);
            
            // Basic Leitner algorithm - move to higher box after consecutive correct answers
            if (progress.getStreak() >= progress.getBox() * 2) {
                progress.setBox(Math.min(5, progress.getBox() + 1));
                progress.setStreak(0);
            }
            
            // Schedule next review based on box (Leitner spaced repetition)
            long daysToAdd = calculateNextReviewDays(progress.getBox());
            progress.setNextReviewAt(LocalDateTime.now().plusDays(daysToAdd));
            
            // Update status based on box level
            if (progress.getBox() >= 4) {
                progress.setStatus(UserVocabProgress.Status.MASTERED);
            } else if (progress.getBox() >= 2) {
                progress.setStatus(UserVocabProgress.Status.LEARNING);
            }
            
        } else {
            // Reset streak and move to box 1
            progress.setStreak(0);
            progress.setBox(1);
            progress.setWrongCount(progress.getWrongCount() + 1);
            progress.setStatus(UserVocabProgress.Status.LEARNING);
            
            // Review again tomorrow if incorrect
            progress.setNextReviewAt(LocalDateTime.now().plusDays(1));
        }

        userVocabProgressRepository.save(progress);
        
        log.info("Updated progress for user {} vocab {}: correct={}, box={}, streak={}", 
                userId, vocabId, correct, progress.getBox(), progress.getStreak());
    }
    
    /**
     * Calculate next review days based on Leitner box
     */
    private long calculateNextReviewDays(Integer box) {
        switch (box) {
            case 1: return 1;    // Review tomorrow
            case 2: return 3;    // Review in 3 days  
            case 3: return 7;    // Review in 1 week
            case 4: return 14;   // Review in 2 weeks
            case 5: return 30;   // Review in 1 month
            default: return 1;
        }
    }

    /**
     * 📊 Lấy session statistics
     */
    public LearningSessionStats getSessionStats(String userId, Integer dictionaryId) {
        User user = userRepository.findByEmail(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Dictionary dictionary = dictionaryRepository.findById(dictionaryId)
            .orElseThrow(() -> new RuntimeException("Dictionary not found: " + dictionaryId));

        // Count total vocabulary in dictionary
        long totalVocabs = vocabRepository.countByDictionary(dictionary);
        
        // Count learned vocabulary (using existing repository method)
        List<UserVocabProgress> userProgress = userVocabProgressRepository.findByUserAndDictionary(user, dictionaryId);
        long learnedVocabs = userProgress.stream()
                .filter(p -> p.getStatus() == UserVocabProgress.Status.MASTERED || p.getBox() >= 3)
                .count();

        // Count due for review
        long dueForReview = userVocabProgressRepository.countDueForReview(user, LocalDateTime.now());

        return LearningSessionStats.builder()
                .totalVocabulary(totalVocabs)
                .learnedVocabulary(learnedVocabs)
                .dueForReview(dueForReview)
                .completionPercentage(totalVocabs > 0 ? (double) learnedVocabs / totalVocabs * 100 : 0)
                .build();
    }

    /**
     * Generate unique session ID
     */
    private String generateSessionId(Long userId, Integer dictionaryId) {
        return String.format("session_%d_%d_%d", 
                userId, dictionaryId, System.currentTimeMillis());
    }

    /**
     * DTO for session statistics
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class LearningSessionStats {
        private long totalVocabulary;
        private long learnedVocabulary;
        private long dueForReview;
        private double completionPercentage;
    }
}
