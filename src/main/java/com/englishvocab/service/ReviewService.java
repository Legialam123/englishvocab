package com.englishvocab.service;

import com.englishvocab.dto.ReviewAnswerResult;
import com.englishvocab.dto.ReviewResultDTO;
import com.englishvocab.dto.ReviewStatsDTO;
import com.englishvocab.dto.VocabWithProgressDTO;
import com.englishvocab.entity.*;
import com.englishvocab.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReviewService {
    
    private final UserVocabProgressRepository progressRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewItemsRepository reviewItemsRepository;
    private final ReviewAttemptsRepository reviewAttemptsRepository;
    private final ReviewItemResultsRepository reviewItemResultsRepository;
    private final VocabRepository vocabRepository;
    
    /**
     * Get review statistics for dashboard
     */
    public ReviewStatsDTO getReviewStats(User user) {
        List<UserVocabProgress> allProgress = progressRepository.findByUser(user);
        
        long overdueCount = allProgress.stream()
            .filter(p -> p.getNextReviewAt() != null && 
                        LocalDateTime.now().isAfter(p.getNextReviewAt()))
            .count();
            
        long todayCount = allProgress.stream()
            .filter(p -> p.getNextReviewAt() != null && 
                        p.getNextReviewAt().toLocalDate().equals(LocalDateTime.now().toLocalDate()))
            .count();
            
        long difficultCount = allProgress.stream()
            .filter(p -> p.getStatus() == UserVocabProgress.Status.DIFFICULT)
            .count();
            
        return ReviewStatsDTO.builder()
            .overdueCount(overdueCount)
            .todayCount(todayCount)
            .difficultCount(difficultCount)
            .totalReviewCount(overdueCount + todayCount)
            .build();
    }
    
    /**
     * Get words that need review most urgently (overdue/today)
     */
    public List<VocabWithProgressDTO> getUrgentReviewWords(User user, int limit) {
        List<UserVocabProgress> allProgress = progressRepository.findByUser(user);
        
        if (allProgress.isEmpty()) {
            log.info("No progress records found for user: {}", user.getEmail());
            return new ArrayList<>();
        }
        
        log.info("Found {} progress records for user: {}", allProgress.size(), user.getEmail());
        
        // Debug: Log all vocab words
        for (UserVocabProgress progress : allProgress) {
            if (progress.getVocab() != null) {
                log.info("Progress vocab: {} - box: {}, nextReview: {}", 
                    progress.getVocab().getWord(), progress.getBox(), progress.getNextReviewAt());
            }
        }
        
        // Get words that need review (overdue or due today)
        List<UserVocabProgress> urgentWords = allProgress.stream()
            .filter(progress -> {
                // Include words that are overdue
                if (progress.getNextReviewAt() != null && 
                    LocalDateTime.now().isAfter(progress.getNextReviewAt())) {
                    return true;
                }
                // Include words that are due today
                if (progress.getNextReviewAt() != null && 
                    progress.getNextReviewAt().toLocalDate().equals(LocalDateTime.now().toLocalDate())) {
                    return true;
                }
                // Include words that have never been reviewed (box = 1)
                if (progress.getBox() == 1) {
                    return true;
                }
                return false;
            })
            .sorted(priorityComparator)
            .limit(limit)
            .collect(Collectors.toList());
        
        log.info("Found {} urgent words for review", urgentWords.size());
        for (UserVocabProgress progress : urgentWords) {
            if (progress.getVocab() != null) {
                log.info("Urgent word: {} - box: {}", progress.getVocab().getWord(), progress.getBox());
            }
        }
        
        List<VocabWithProgressDTO> result = urgentWords.stream()
            .map(progress -> {
                VocabWithProgressDTO dto = VocabWithProgressDTO.of(progress.getVocab(), progress);
                log.info("Created DTO for word: {} (vocab_id: {})", 
                    dto.getWord(), progress.getVocab() != null ? progress.getVocab().getVocabId() : "NULL");
                return dto;
            })
            .collect(Collectors.toList());
            
        log.info("Final result contains {} DTOs", result.size());
        for (VocabWithProgressDTO dto : result) {
            log.info("Final DTO word: {} (vocab_id: {})", 
                dto.getWord(), dto.getVocab() != null ? dto.getVocab().getVocabId() : "NULL");
        }
        
        return result;
    }
    
    /**
     * Get recently learned words (box 2-4) for practice
     */
    public List<VocabWithProgressDTO> getRecentLearnedWords(User user, int limit) {
        List<UserVocabProgress> allProgress = progressRepository.findByUser(user);
        
        if (allProgress.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Get words that have been learned recently (box 2-4)
        List<UserVocabProgress> recentWords = allProgress.stream()
            .filter(progress -> progress.getBox() >= 2 && progress.getBox() <= 4)
            .sorted((p1, p2) -> {
                // Sort by last reviewed date (most recent first)
                if (p1.getLastReviewed() == null && p2.getLastReviewed() == null) return 0;
                if (p1.getLastReviewed() == null) return 1;
                if (p2.getLastReviewed() == null) return -1;
                return p2.getLastReviewed().compareTo(p1.getLastReviewed());
            })
            .limit(limit)
            .collect(Collectors.toList());
        
        return recentWords.stream()
            .map(progress -> VocabWithProgressDTO.of(progress.getVocab(), progress))
            .collect(Collectors.toList());
    }
    
    /**
     * Get words for review with smart logic
     */
    public List<VocabWithProgressDTO> getReviewWords(User user, int limit) {
        // 1. Try urgent words first (overdue/today)
        List<VocabWithProgressDTO> urgentWords = getUrgentReviewWords(user, limit);
        if (!urgentWords.isEmpty()) {
            return urgentWords;
        }
        
        // 2. Try recently learned words (box 2-4)
        List<VocabWithProgressDTO> recentWords = getRecentLearnedWords(user, limit);
        if (!recentWords.isEmpty()) {
            return recentWords;
        }
        
        // 3. No words available for review
        return new ArrayList<>();
    }
    
    /**
     * Create a vocabulary review session
     */
    public Reviews createVocabularyReview(User user, List<VocabWithProgressDTO> words) {
        log.info("Creating review session with {} words", words.size());
        for (VocabWithProgressDTO word : words) {
            log.info("Review word: {} (vocab_id: {})", 
                word.getWord(), word.getVocab() != null ? word.getVocab().getVocabId() : "NULL");
        }
        
        Reviews review = Reviews.builder()
            .title("Ôn tập từ vựng - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
            .status(Reviews.Status.ACTIVE)
            .reviewType(Reviews.ReviewType.VOCABULARY_REVIEW)
            .timeLimitSec(1080) // 18 phút
            .numItems(15)
            .passScore(70)
            .build();
            
        review = reviewRepository.save(review);
        
        // Create 15 review questions (5 Multiple Choice + 5 True/False + 5 Fill in Blank)
        createReviewQuestions(review, words);
        
        return review;
    }
    
    /**
     * Create review questions for 3 modes
     */
    private void createReviewQuestions(Reviews review, List<VocabWithProgressDTO> words) {
        try {
            log.info("Creating review questions for review ID: {} with {} words", review.getReviewId(), words.size());
            
            List<ReviewItems> questions = new ArrayList<>();
            int wordCount = words.size();
            
            // Calculate questions per type based on available words
            int multipleChoiceCount = Math.min(5, wordCount);
            int trueFalseCount = Math.min(5, Math.max(0, wordCount - 5));
            int fillInBlankCount = Math.min(5, Math.max(0, wordCount - 10));
            
            log.info("Question distribution: MC={}, TF={}, FB={}", multipleChoiceCount, trueFalseCount, fillInBlankCount);
            
            // Multiple Choice questions
            for (int i = 0; i < multipleChoiceCount; i++) {
                VocabWithProgressDTO word = words.get(i);
                log.info("Creating MC question for word: {} (vocab_id: {})", 
                    word.getWord(), word.getVocab() != null ? word.getVocab().getVocabId() : "NULL");
                ReviewItems question = createMultipleChoiceQuestion(review, word);
                question.setSessionOrder(i);
                questions.add(question);
            }
            
            // True/False questions
            for (int i = multipleChoiceCount; i < multipleChoiceCount + trueFalseCount; i++) {
                VocabWithProgressDTO word = words.get(i);
                log.info("Creating TF question for word: {} (vocab_id: {})", 
                    word.getWord(), word.getVocab() != null ? word.getVocab().getVocabId() : "NULL");
                ReviewItems question = createTrueFalseQuestion(review, word);
                question.setSessionOrder(i);
                questions.add(question);
            }
            
            // Fill in Blank questions
            for (int i = multipleChoiceCount + trueFalseCount; i < multipleChoiceCount + trueFalseCount + fillInBlankCount; i++) {
                VocabWithProgressDTO word = words.get(i);
                log.info("Creating FB question for word: {} (vocab_id: {})", 
                    word.getWord(), word.getVocab() != null ? word.getVocab().getVocabId() : "NULL");
                ReviewItems question = createFillInBlankQuestion(review, word);
                question.setSessionOrder(i);
                questions.add(question);
            }
            
            log.info("Created {} questions, saving to database", questions.size());
            
            // Debug: Log question types
            for (int i = 0; i < questions.size(); i++) {
                ReviewItems question = questions.get(i);
                log.info("Question {}: type={}, word={}, answer={}", 
                    i + 1, question.getType(), 
                    question.getVocab() != null ? question.getVocab().getWord() : "NULL",
                    question.getAnswer());
            }
            
            // Update review with actual number of items
            review.setNumItems(questions.size());
            reviewRepository.save(review);
            
            // Save questions one by one to ensure order
            for (int i = 0; i < questions.size(); i++) {
                ReviewItems question = questions.get(i);
                question = reviewItemsRepository.save(question);
                questions.set(i, question); // Update with saved entity
                log.info("Saved question {}: id={}, type={}, word={}", 
                    i + 1, question.getReviewItemId(), question.getType(),
                    question.getVocab() != null ? question.getVocab().getWord() : "NULL");
            }
            log.info("Successfully saved {} review questions", questions.size());
            
        } catch (Exception e) {
            log.error("Error creating review questions for review ID: {}", review.getReviewId(), e);
            throw e;
        }
    }
    
    /**
     * Create Multiple Choice question
     */
    private ReviewItems createMultipleChoiceQuestion(Reviews review, VocabWithProgressDTO word) {
        // Get correct meaning - force load senses to avoid lazy loading issues
        String correctMeaning = getCorrectMeaning(word.getVocab());
        
        log.info("Creating MC question for word: {} with meaning: {}", word.getWord(), correctMeaning);
        
        // Get 3 wrong meanings from database
        List<String> wrongMeanings = getRandomMeanings(correctMeaning, 3);
        
        // Create 4 options: 1 correct + 3 wrong (without A/B/C/D prefix)
        List<String> options = new ArrayList<>();
        options.add(correctMeaning);
        options.addAll(wrongMeanings);
        
        // Shuffle options to randomize position
        Collections.shuffle(options);
        
        // Find correct answer index after shuffling
        int correctIndex = options.indexOf(correctMeaning);
        
        log.info("MC question options: {} (correct at index: {})", options, correctIndex);
        log.info("MC question - word: {}, correctMeaning: {}, correctIndex: {}", 
            word.getWord(), correctMeaning, correctIndex);
        
        return ReviewItems.builder()
            .review(review)
            .vocab(word.getVocab())
            .customVocab(null) // System vocabulary, not custom
            .type(ReviewItems.Type.MULTIPLE_CHOICE)
            .prompt(word.getWord())
            .option(String.join("|", options)) // Store without A/B/C/D prefix
            .answer(String.valueOf(correctIndex)) // Store as index (0-3)
            .difficulty(ReviewItems.Difficulty.MEDIUM)
            .build();
    }
    
    /**
     * Create True/False question
     */
    private ReviewItems createTrueFalseQuestion(Reviews review, VocabWithProgressDTO word) {
        Random random = new Random();
        boolean isCorrect = random.nextBoolean();
        
        String correctMeaning = getCorrectMeaning(word.getVocab());
        String prompt;
        String answer;
        
        if (isCorrect) {
            // Case 1: Word + correct meaning -> User should answer TRUE
            prompt = word.getWord() + " có nghĩa là " + correctMeaning;
            answer = "TRUE";
        } else {
            // Case 2: Word + wrong meaning -> User should answer FALSE
            String wrongMeaning = getRandomMeaning(correctMeaning);
            prompt = word.getWord() + " có nghĩa là " + wrongMeaning;
            answer = "FALSE";
        }
        
        return ReviewItems.builder()
            .review(review)
            .vocab(word.getVocab())
            .customVocab(null) // System vocabulary, not custom
            .type(ReviewItems.Type.TRUE_FALSE)
            .prompt(prompt)
            .answer(answer)
            .difficulty(ReviewItems.Difficulty.EASY)
            .build();
    }
    
    /**
     * Create Fill in Blank question
     */
    private ReviewItems createFillInBlankQuestion(Reviews review, VocabWithProgressDTO word) {
        String correctMeaning = getCorrectMeaning(word.getVocab());
        String prompt = "Nghe: " + word.getIpa() + " - Nghĩa: " + correctMeaning;
        
        return ReviewItems.builder()
            .review(review)
            .vocab(word.getVocab())
            .customVocab(null) // System vocabulary, not custom
            .type(ReviewItems.Type.FILL_IN_BLANK)
            .prompt(prompt)
            .answer(word.getWord())
            .difficulty(ReviewItems.Difficulty.HARD)
            .build();
    }
    
    /**
     * Get random meanings from database (excluding the correct answer)
     */
    private List<String> getRandomMeanings(String correctMeaning, int count) {
        try {
            // Get random meanings from database, excluding the correct one
            List<String> randomMeanings = vocabRepository.findRandomMeaningsExcluding(correctMeaning, count * 2);
            
            // If we don't have enough from database, fill with fallback meanings
            if (randomMeanings.size() < count) {
                List<String> fallbackMeanings = Arrays.asList(
                    "từ đồng nghĩa", "từ trái nghĩa", "nghĩa gần giống",
                    "nghĩa khác", "từ liên quan", "nghĩa tương tự"
                );
                
                // Add fallback meanings that aren't already included
                for (String fallback : fallbackMeanings) {
                    if (!randomMeanings.contains(fallback) && randomMeanings.size() < count) {
                        randomMeanings.add(fallback);
                    }
                }
            }
            
            // Shuffle and return the requested count
            Collections.shuffle(randomMeanings);
            return randomMeanings.subList(0, Math.min(count, randomMeanings.size()));
            
        } catch (Exception e) {
            log.warn("Error getting random meanings from database, using fallback: {}", e.getMessage());
            
            // Fallback to hardcoded meanings
            List<String> fallbackMeanings = Arrays.asList(
                "từ đồng nghĩa", "từ trái nghĩa", "nghĩa gần giống",
                "nghĩa khác", "từ liên quan", "nghĩa tương tự"
            );
            
            Collections.shuffle(fallbackMeanings);
            return fallbackMeanings.subList(0, Math.min(count, fallbackMeanings.size()));
        }
    }
    
    /**
     * Get a single random meaning (for True/False questions)
     */
    private String getRandomMeaning(String correctMeaning) {
        List<String> randomMeanings = getRandomMeanings(correctMeaning, 1);
        return randomMeanings.isEmpty() ? "nghĩa khác" : randomMeanings.get(0);
    }
    
    /**
     * Get correct meaning from vocab, handling lazy loading
     */
    private String getCorrectMeaning(Vocab vocab) {
        if (vocab == null) {
            return "N/A";
        }
        
        try {
            // Force load senses by accessing the collection
            if (vocab.getSenses() != null && !vocab.getSenses().isEmpty()) {
                String meaning = vocab.getSenses().get(0).getMeaningVi();
                log.info("Loaded meaning for vocab {}: {}", vocab.getWord(), meaning);
                return meaning;
            } else {
                log.warn("No senses found for vocab: {}", vocab.getWord());
                return "N/A";
            }
        } catch (Exception e) {
            log.error("Error loading meaning for vocab {}: {}", vocab.getWord(), e.getMessage());
            return "N/A";
        }
    }
    
    /**
     * Generate wrong answers for multiple choice (legacy method - deprecated)
     * @deprecated Use getRandomMeanings() instead
     */
    @Deprecated
    private List<String> generateWrongAnswers(String correctAnswer) {
        return getRandomMeanings(correctAnswer, 3);
    }
    
    /**
     * Start review attempt
     */
    public ReviewAttempts startReviewAttempt(User user, Reviews review) {
        return reviewAttemptsRepository.save(ReviewAttempts.builder()
            .user(user)
            .review(review)
            .attemptType(ReviewAttempts.AttemptType.REVIEW)
            .startedAt(LocalDateTime.now())
            .maxScore(review.getNumItems())
            .build());
    }
    
    /**
     * Process answer and update progress
     */
    public void processAnswer(Integer attemptId, Integer itemId, String userAnswer) {
        ReviewAttempts attempt = reviewAttemptsRepository.findById(attemptId)
            .orElseThrow(() -> new RuntimeException("Attempt not found"));
            
        ReviewItems item = reviewItemsRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Item not found"));
            
        boolean isCorrect = checkAnswer(item, userAnswer);
        
        // Save result
        reviewItemResultsRepository.save(ReviewItemResults.builder()
            .reviewItem(item)
            .reviewAttempt(attempt)
            .isCorrect(isCorrect)
            .score(isCorrect ? 1 : 0)
            .userAnswer(userAnswer)
            .build());
            
        // Update UserVocabProgress
        if (item.getVocab() != null) {
            log.info("Updating progress for vocab: {} (vocab_id: {})", 
                item.getVocab().getWord(), item.getVocab().getVocabId());
            updateUserProgress(attempt.getUser(), item.getVocab(), isCorrect);
        } else {
            log.warn("Item vocab is NULL for itemId: {}", itemId);
        }
    }
    
    /**
     * Process answer and return detailed result
     */
    public ReviewAnswerResult processAnswerWithResult(Integer attemptId, Integer itemId, String userAnswer) {
        ReviewAttempts attempt = reviewAttemptsRepository.findById(attemptId)
            .orElseThrow(() -> new RuntimeException("Attempt not found"));
            
        // Try to find ReviewItems by ID first, then by sessionOrder as fallback
        ReviewItems item = reviewItemsRepository.findById(itemId)
            .orElseGet(() -> {
                log.warn("ReviewItems not found by ID: {}, trying to find by sessionOrder", itemId);
                // If itemId looks like a sessionOrder (1-15), try to find by sessionOrder
                if (itemId >= 1 && itemId <= 15) {
                    return reviewItemsRepository.findByReviewAndSessionOrder(attempt.getReview(), itemId - 1)
                        .orElseThrow(() -> new RuntimeException("Item not found by sessionOrder: " + (itemId - 1)));
                }
                throw new RuntimeException("Item not found by ID: " + itemId);
            });
            
        log.info("Processing answer for itemId: {}, actualReviewItemId: {}, word: {}, type: {}, answer: {}, userAnswer: {}", 
            itemId, 
            item.getReviewItemId(),
            item.getVocab() != null ? item.getVocab().getWord() : "NULL",
            item.getType(),
            item.getAnswer(),
            userAnswer);
            
        boolean isCorrect = checkAnswer(item, userAnswer);
        
        // Save result
        reviewItemResultsRepository.save(ReviewItemResults.builder()
            .reviewItem(item)
            .reviewAttempt(attempt)
            .isCorrect(isCorrect)
            .score(isCorrect ? 1 : 0)
            .userAnswer(userAnswer)
            .build());
            
        // Update UserVocabProgress
        if (item.getVocab() != null) {
            log.info("Updating progress for vocab: {} (vocab_id: {})", 
                item.getVocab().getWord(), item.getVocab().getVocabId());
            updateUserProgress(attempt.getUser(), item.getVocab(), isCorrect);
        } else {
            log.warn("Item vocab is NULL for itemId: {}", itemId);
        }
        
        // Prepare result details based on question type
        String word = item.getVocab() != null ? item.getVocab().getWord() : "Unknown";
        String meaning = item.getVocab() != null ? item.getVocab().getPrimaryMeaning() : "Unknown";
        
        log.info("Processing result for itemId: {}, word: {}, type: {}, answer: {}", 
            itemId, word, item.getType(), item.getAnswer());
        
        return switch (item.getType()) {
            case MULTIPLE_CHOICE -> {
                log.info("Processing MULTIPLE_CHOICE case for itemId: {}", itemId);
                String[] options = item.getOptionsArray();
                int correctIndex;
                try {
                    correctIndex = Integer.parseInt(item.getAnswer());
                } catch (NumberFormatException e) {
                    // Fallback: convert letter to index (A=0, B=1, C=2, D=3)
                    String answer = item.getAnswer().trim().toUpperCase();
                    if (answer.length() == 1 && answer.charAt(0) >= 'A' && answer.charAt(0) <= 'D') {
                        correctIndex = answer.charAt(0) - 'A';
                    } else {
                        correctIndex = 0; // Default to first option
                    }
                }
                String correctText = options[correctIndex];
                log.info("MC Result - word: {}, options: {}, correctIndex: {}, correctText: {}", 
                    word, java.util.Arrays.toString(options), correctIndex, correctText);
                yield ReviewAnswerResult.multipleChoice(isCorrect, userAnswer, correctIndex, correctText, word, meaning);
            }
            case TRUE_FALSE -> {
                log.info("Processing TRUE_FALSE case for itemId: {}", itemId);
                String correctAnswer = item.getAnswer();
                log.info("TF Result - word: {}, correctAnswer: {}", word, correctAnswer);
                yield ReviewAnswerResult.trueFalse(isCorrect, userAnswer, correctAnswer, word, meaning);
            }
            case FILL_IN_BLANK -> {
                log.info("Processing FILL_IN_BLANK case for itemId: {}", itemId);
                String correctWord = item.getAnswer();
                log.info("FB Result - word: {}, correctWord: {}", word, correctWord);
                yield ReviewAnswerResult.fillInBlank(isCorrect, userAnswer, correctWord, word, meaning);
            }
            default -> {
                log.warn("Processing DEFAULT case for itemId: {}, type: {}", itemId, item.getType());
                // Fallback to generic result
                String correctAnswer = item.getAnswer();
                yield ReviewAnswerResult.builder()
                    .correct(isCorrect)
                    .userAnswer(userAnswer)
                    .questionType(item.getType().name())
                    .word(word)
                    .meaning(meaning)
                    .explanation(isCorrect ? "Chính xác! 🎉" : "Sai rồi! Đáp án đúng là: " + correctAnswer)
                    .build();
            }
        };
    }
    
    
    /**
     * Check if answer is correct
     */
    private boolean checkAnswer(ReviewItems item, String userAnswer) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) return false;
        
        return switch (item.getType()) {
            case MULTIPLE_CHOICE -> {
                try {
                    // User answer is index (0-3), compare with stored answer index
                    int userIndex = Integer.parseInt(userAnswer.trim());
                    int correctIndex = Integer.parseInt(item.getAnswer());
                    log.info("MC Answer Check - word: {}, userIndex: {}, correctIndex: {}, match: {}", 
                        item.getVocab() != null ? item.getVocab().getWord() : "NULL", 
                        userIndex, correctIndex, userIndex == correctIndex);
                    yield userIndex == correctIndex;
                } catch (NumberFormatException e) {
                    // Fallback: direct comparison (for backward compatibility)
                    log.warn("MC Answer Check - NumberFormatException for userAnswer: {}, itemAnswer: {}", 
                        userAnswer, item.getAnswer());
                    yield userAnswer.equalsIgnoreCase(item.getAnswer());
                }
            }
            case TRUE_FALSE -> userAnswer.trim().equalsIgnoreCase(item.getAnswer());
            case FILL_IN_BLANK -> userAnswer.trim().equalsIgnoreCase(item.getAnswer().trim());
            default -> false;
        };
    }
    
    /**
     * Update UserVocabProgress based on answer
     */
    private void updateUserProgress(User user, Vocab vocab, boolean isCorrect) {
        try {
            log.info("updateUserProgress called for user: {}, vocab: {} (vocab_id: {}), isCorrect: {}", 
                user.getEmail(), vocab.getWord(), vocab.getVocabId(), isCorrect);
                
            // Find existing progress for this vocab
            UserVocabProgress progress = progressRepository.findByUserAndVocab(user, vocab)
                .orElse(null);
                
            if (progress == null) {
                // Create new progress if doesn't exist
                log.info("Creating new progress for vocab: {} (vocab_id: {})", vocab.getWord(), vocab.getVocabId());
                progress = UserVocabProgress.builder()
                    .user(user)
                    .vocab(vocab)
                    .box(1)
                    .streak(0)
                    .wrongCount(0)
                    .status(UserVocabProgress.Status.LEARNING)
                    .lastReviewed(LocalDateTime.now())
                    .nextReviewAt(LocalDateTime.now().plusDays(1))
                    .build();
            } else {
                log.info("Found existing progress for vocab: {} (vocab_id: {}), current box: {}", 
                    vocab.getWord(), vocab.getVocabId(), progress.getBox());
            }
            
            if (isCorrect) {
                progress.setStreak(progress.getStreak() + 1);
                progress.setBox(Math.min(progress.getBox() + 1, 5)); // Max box 5
                if (progress.getBox() >= 3) {
                    progress.setStatus(UserVocabProgress.Status.MASTERED);
                } else {
                    progress.setStatus(UserVocabProgress.Status.LEARNING);
                }
            } else {
                progress.setWrongCount(progress.getWrongCount() + 1);
                progress.setStreak(0);
                progress.setBox(Math.max(progress.getBox() - 1, 1)); // Min box 1
                progress.setStatus(UserVocabProgress.Status.REVIEWING);
            }
            
            progress.setLastReviewed(LocalDateTime.now());
            progress.setNextReviewAt(LocalDateTime.now().plusDays(progress.getBox()));
            
            progressRepository.save(progress);
            log.info("Updated progress for vocab {}: correct={}, box={}, streak={}", 
                vocab.getWord(), isCorrect, progress.getBox(), progress.getStreak());
                
        } catch (Exception e) {
            log.error("Error updating progress for vocab {}: {}", vocab.getWord(), e.getMessage());
        }
    }
    
    /**
     * Calculate review results
     */
    public ReviewResultDTO calculateReviewResults(Integer attemptId) {
        ReviewAttempts attempt = reviewAttemptsRepository.findById(attemptId)
            .orElseThrow(() -> new RuntimeException("Attempt not found"));
            
        List<ReviewItemResults> results = reviewItemResultsRepository.findByReviewAttempt(attempt);
        
        List<String> masteredWords = new ArrayList<>();
        List<String> needReviewWords = new ArrayList<>();
        
        for (ReviewItemResults result : results) {
            String word;
            if (result.getReviewItem().getVocab() != null) {
                word = result.getReviewItem().getVocab().getWord();
            } else if (result.getReviewItem().getCustomVocab() != null) {
                word = result.getReviewItem().getCustomVocab().getName();
            } else {
                word = "Unknown";
            }
            
            if (result.getIsCorrect()) {
                masteredWords.add(word);
            } else {
                needReviewWords.add(word);
            }
        }
        
        // Update attempt
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setScore(masteredWords.size());
        if (attempt.getStartedAt() != null) {
            attempt.setDurationSec((int) Duration.between(attempt.getStartedAt(), LocalDateTime.now()).getSeconds());
        }
        reviewAttemptsRepository.save(attempt);
        
        return ReviewResultDTO.builder()
            .totalCorrect(masteredWords.size())
            .totalQuestions(attempt.getMaxScore())
            .masteredWords(masteredWords)
            .needReviewWords(needReviewWords)
            .duration(Duration.between(attempt.getStartedAt(), LocalDateTime.now()))
            .build();
    }
    
    /**
     * Priority comparator for review words
     */
    private final Comparator<UserVocabProgress> priorityComparator = (p1, p2) -> {
        // Calculate priority scores
        int score1 = calculatePriorityScore(p1);
        int score2 = calculatePriorityScore(p2);
        return Integer.compare(score2, score1); // Higher score = higher priority
    };
    
    private int calculatePriorityScore(UserVocabProgress progress) {
        int score = 0;
        
        // Overdue days (higher = more urgent)
        if (progress.getNextReviewAt() != null && LocalDateTime.now().isAfter(progress.getNextReviewAt())) {
            long overdueDays = java.time.temporal.ChronoUnit.DAYS.between(
                progress.getNextReviewAt(), LocalDateTime.now());
            score += (int) (overdueDays * 10);
        }
        
        // Wrong count (higher = more urgent)
        score += progress.getWrongCount() * 5;
        
        // Lower box = more urgent
        score += (6 - progress.getBox()) * 2;
        
        // Days since last review
        if (progress.getLastReviewed() != null) {
            long daysSinceReview = java.time.temporal.ChronoUnit.DAYS.between(
                progress.getLastReviewed(), LocalDateTime.now());
            score += (int) daysSinceReview;
        }
        
        return score;
    }
    
    /**
     * Get all user progress records (for debugging)
     */
    public List<UserVocabProgress> getUserProgress(User user) {
        return progressRepository.findByUser(user);
    }
    
    /**
     * Get review by ID
     */
    public Reviews getReviewById(Integer reviewId) {
        return reviewRepository.findById(reviewId)
            .orElseThrow(() -> new RuntimeException("Review not found"));
    }
    
    /**
     * Get review questions by review ID
     */
    public List<ReviewItems> getReviewQuestions(Integer reviewId) {
        // Get questions and sort by creation order (not by ID)
        List<ReviewItems> questions = reviewItemsRepository.findByReviewOrderByCreatedAtAsc(reviewId);
        log.info("Retrieved {} questions for reviewId: {}", questions.size(), reviewId);
        for (int i = 0; i < questions.size(); i++) {
            ReviewItems q = questions.get(i);
            log.info("Question {}: id={}, type={}, word={}", 
                i, q.getReviewItemId(), q.getType(), 
                q.getVocab() != null ? q.getVocab().getWord() : "NULL");
        }
        return questions;
    }
    
    /**
     * Get last review date for user
     */
    public String getLastReviewDate(User user) {
        Optional<ReviewAttempts> lastAttempt = reviewAttemptsRepository
            .findTopByUserOrderByStartedAtDesc(user);
            
        if (lastAttempt.isPresent()) {
            LocalDateTime lastDate = lastAttempt.get().getStartedAt();
            LocalDateTime now = LocalDateTime.now();
            long daysBetween = ChronoUnit.DAYS.between(lastDate.toLocalDate(), now.toLocalDate());
            
            if (daysBetween == 0) {
                return "Hôm nay";
            } else if (daysBetween == 1) {
                return "1 ngày trước";
            } else {
                return daysBetween + " ngày trước";
            }
        }
        
        return null;
    }
    
    /**
     * Get review results by attempt ID
     */
    public ReviewResultDTO getReviewResults(Integer attemptId) {
        return calculateReviewResults(attemptId);
    }
    
    /**
     * Get last review result for user
     */
    public ReviewResultDTO getLastReviewResult(User user) {
        Optional<ReviewAttempts> lastAttempt = reviewAttemptsRepository
            .findTopByUserOrderByStartedAtDesc(user);
            
        if (lastAttempt.isPresent()) {
            return getReviewResults(lastAttempt.get().getReviewAttemptId());
        }
        
        return null;
    }
}
