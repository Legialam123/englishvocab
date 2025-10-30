package com.englishvocab.service;

import com.englishvocab.dto.SessionResultDTO;
import com.englishvocab.dto.SessionResultRequest;
import com.englishvocab.entity.*;
import com.englishvocab.repository.LearningSessionRepository;
import com.englishvocab.repository.SessionVocabularyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service quản lý Learning Sessions
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LearningService {
    
    private final LearningSessionRepository sessionRepository;
    private final SessionVocabularyRepository sessionVocabRepository;
    private final VocabularyService vocabularyService;
    private final UserProgressService userProgressService;
    
    private static final int SESSION_TIMEOUT_MINUTES = 30;
    private static final int SESSION_CLEANUP_DAYS = 30;
    private static final int BATCH_SIZE = 100;

    // ==================== SESSION CREATION ====================
    
    /**
     * Tạo learning session mới với auto-expire sau 30 phút
     */
    public LearningSession createSession(User user, com.englishvocab.entity.Dictionary dictionary, 
                                        String learningMode, List<Integer> vocabularyIds, 
                                        Integer maxVocabularies) {
        return createSession(user, dictionary, learningMode, vocabularyIds, maxVocabularies, null, null, null);
    }
    
    /**
     * Tạo learning session mới với auto-expire sau 30 phút (với level và startLetter filters)
     */
    public LearningSession createSession(User user, com.englishvocab.entity.Dictionary dictionary, 
                                        String learningMode, List<Integer> vocabularyIds, 
                                        Integer maxVocabularies, String level, String startLetter) {
        return createSession(user, dictionary, learningMode, vocabularyIds, maxVocabularies, level, startLetter, null);
    }
    
    /**
     * Tạo learning session mới với auto-expire sau 30 phút (với tất cả filters)
     */
    public LearningSession createSession(User user, com.englishvocab.entity.Dictionary dictionary, 
                                        String learningMode, List<Integer> vocabularyIds, 
                                        Integer maxVocabularies, String level, String startLetter, List<Integer> topicIds) {
        LearningSession.LearningMode requestedMode = parseLearningMode(learningMode);

        if (hasActiveSession(user)) {
            List<LearningSession> activeSessions = sessionRepository.findActiveOrPausedSessions(user);

            activeSessions.stream()
                .filter(LearningSession::isExpired)
                .forEach(LearningSession::expire);

            Optional<LearningSession> reusableSession = activeSessions.stream()
                .filter(session -> !session.isExpired()
                        && session.getDictionary().getDictionaryId().equals(dictionary.getDictionaryId())
                        && session.getLearningMode() == requestedMode)
                .findFirst();

            if (reusableSession.isPresent()) {
                LearningSession activeSession = reusableSession.get();

                if (activeSession.getStatus() == LearningSession.Status.PAUSED) {
                    activeSession.resume();
                } else {
                    activeSession.setExpiresAt(LocalDateTime.now().plusMinutes(SESSION_TIMEOUT_MINUTES));
                    activeSession.updateActivity();
                }

                sessionRepository.saveAll(activeSessions);

                return sessionRepository.save(activeSession);
            }

            activeSessions.stream()
                .filter(session -> !session.isExpired())
                .forEach(LearningSession::cancel);

            sessionRepository.saveAll(activeSessions);
        }

        // Select vocabularies theo priority with all filters
        List<Vocab> vocabularies = selectVocabulariesForSession(
            user, dictionary, learningMode, vocabularyIds, maxVocabularies, level, startLetter, topicIds);

        if (vocabularies.isEmpty()) {
            log.warn("No vocabularies found for user {} dictionary {} mode {} with requestedIds {} level {} startLetter {} topicIds {}",
                    user.getEmail(), dictionary.getDictionaryId(), requestedMode, vocabularyIds, level, startLetter, topicIds);
            throw new RuntimeException("Không tìm thấy từ vựng phù hợp!");
        }

        // Create session với timeout 30 phút
        LearningSession session = new LearningSession();
        session.setUser(user);
        session.setDictionary(dictionary);
        session.setSessionUuid(UUID.randomUUID().toString());
        session.setStatus(LearningSession.Status.ACTIVE);
        session.setLearningMode(requestedMode);
        session.setTargetWords(vocabularies.size());
        session.setActualWords(0);
        session.setStartedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusMinutes(SESSION_TIMEOUT_MINUTES));
        
        session = sessionRepository.save(session);

        // Create SessionVocabulary items
        List<SessionVocabulary> sessionVocabs = new ArrayList<>();
        for (int i = 0; i < vocabularies.size(); i++) {
            SessionVocabulary sv = new SessionVocabulary();
            sv.setSession(session);
            sv.setVocab(vocabularies.get(i));
            sv.setOrderIndex(i);
            sessionVocabs.add(sv);
        }
        sessionVocabRepository.saveAll(sessionVocabs);

        log.info("Created learning session {} for user {} ({} vocabularies)",
            session.getSessionUuid(), user.getEmail(), vocabularies.size());

        return session;
    }

    /**
     * Parse learning mode string to enum
     */
    private LearningSession.LearningMode parseLearningMode(String mode) {
        if (mode == null) {
            return LearningSession.LearningMode.REVIEW;
        }
        return switch (mode.toLowerCase()) {
            case "alphabetical" -> LearningSession.LearningMode.ALPHABETICAL;
            case "topics" -> LearningSession.LearningMode.TOPICS;
            case "custom" -> LearningSession.LearningMode.CUSTOM;
            case "review" -> LearningSession.LearningMode.REVIEW;
            default -> LearningSession.LearningMode.REVIEW;
        };
    }

    /**
     * Select vocabularies với priority: Review > New > Random (without filters)
     */
    private List<Vocab> selectVocabulariesForSession(User user, com.englishvocab.entity.Dictionary dictionary, 
                                                     String learningMode, List<Integer> vocabularyIds, 
                                                     Integer maxVocabularies) {
        return selectVocabulariesForSession(user, dictionary, learningMode, vocabularyIds, maxVocabularies, null, null, null);
    }
    
    /**
     * Select vocabularies với priority: Review > New > Random (with all filters)
     */
    private List<Vocab> selectVocabulariesForSession(User user, com.englishvocab.entity.Dictionary dictionary, 
                                                     String learningMode, List<Integer> vocabularyIds, 
                                                     Integer maxVocabularies, String level, String startLetter, List<Integer> topicIds) {
        List<Vocab> selected = new ArrayList<>();

        switch (learningMode.toLowerCase()) {
            case "review":
                // Ưu tiên từ cần review
                selected = userProgressService.getVocabulariesDueForReview(user, dictionary, maxVocabularies);
                break;

            case "new":
                // Ưu tiên từ chưa học
                selected = userProgressService.getNewVocabularies(user, dictionary, maxVocabularies);
                break;

            case "custom":
                // Từ user chọn
                if (vocabularyIds != null && !vocabularyIds.isEmpty()) {
                    selected = vocabularyService.findByIdIn(vocabularyIds);
                }
                break;

            case "alphabetical":
                // A-Z order with level and startLetter filters
                if (level != null && !level.isEmpty() && startLetter != null && !startLetter.isEmpty()) {
                    // Both filters
                    selected = vocabularyService.getVocabulariesByDictionaryAndLevelAndLetter(
                        dictionary, level, startLetter, maxVocabularies);
                } else if (level != null && !level.isEmpty()) {
                    // Only level filter
                    selected = vocabularyService.getVocabulariesByDictionaryAndLevel(
                        dictionary, level, maxVocabularies);
                } else if (startLetter != null && !startLetter.isEmpty()) {
                    // Only letter filter
                    selected = vocabularyService.getVocabulariesByDictionaryAndLetter(
                        dictionary, startLetter, maxVocabularies);
                } else {
                    // No filters
                    selected = vocabularyService.getVocabulariesByDictionary(dictionary, maxVocabularies);
                }
                break;

            case "topics":
                // Topics-based learning with optional level filter
                if (topicIds != null && !topicIds.isEmpty()) {
                    selected = vocabularyService.getVocabulariesByDictionaryAndTopics(
                        dictionary, topicIds, level, maxVocabularies);
                } else {
                    log.warn("Topics mode requested but no topicIds provided, falling back to random");
                    selected = vocabularyService.getRandomVocabularies(dictionary, maxVocabularies);
                }
                break;

            case "random":
            default:
                // Random
                selected = vocabularyService.getRandomVocabularies(dictionary, maxVocabularies);
                break;
        }

        // For alphabetical and topics modes with filters, don't fill with random words
        // Just use what we got from the filter
        boolean hasFilters = (level != null && !level.isEmpty()) || 
                            (startLetter != null && !startLetter.isEmpty()) ||
                            (topicIds != null && !topicIds.isEmpty());
        boolean isAlphabetical = "alphabetical".equalsIgnoreCase(learningMode);
        boolean isTopics = "topics".equalsIgnoreCase(learningMode);
        
        // Only fill with random if not alphabetical/topics mode or no filters
        if (!isAlphabetical && !isTopics && selected.size() < maxVocabularies) {
            List<Vocab> additional = vocabularyService.getRandomVocabularies(
                dictionary, maxVocabularies - selected.size());
            selected.addAll(additional);
        }

        // Shuffle để tránh predictable (trừ alphabetical mode)
        if (!isAlphabetical) {
            Collections.shuffle(selected);
        }

        return selected.stream()
            .limit(maxVocabularies)
            .collect(Collectors.toList());
    }

    /**
     * Kiểm tra user có session đang active không
     */
    public boolean hasActiveSession(User user) {
        return sessionRepository.hasActiveSession(user);
    }

    /**
     * Lấy active session của user
     */
    public Optional<LearningSession> getActiveSession(User user) {
        List<LearningSession> sessions = sessionRepository.findActiveOrPausedSessions(user);
        return sessions.isEmpty() ? Optional.empty() : Optional.of(sessions.get(0));
    }
    
    /**
     * Get session by UUID
     */
    public LearningSession getSessionByUuid(String sessionUuid) {
        return sessionRepository.findBySessionUuid(sessionUuid)
            .orElseThrow(() -> new RuntimeException("Session không tồn tại"));
    }

    // ==================== SESSION ACTIONS ====================

    /**
     * Record câu trả lời của user
     */
    public LearningSession recordAnswer(String sessionUuid, Integer vocabularyId, 
                            SessionVocabulary.AnswerType answer, Integer timeSpentSec) {
        LearningSession session = sessionRepository.findBySessionUuid(sessionUuid)
            .orElseThrow(() -> new RuntimeException("Session không tồn tại"));

        // Check expired
        if (session.isExpired()) {
            throw new RuntimeException("Session đã hết hạn");
        }

        // Find SessionVocabulary
        List<SessionVocabulary> sessionVocabs = sessionVocabRepository.findBySessionOrderByOrderIndex(session);
        SessionVocabulary sessionVocab = sessionVocabs.stream()
            .filter(sv -> sv.getVocab().getVocabId().equals(vocabularyId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Vocabulary không thuộc session này"));

        // Record answer
        sessionVocab.recordAnswer(answer, timeSpentSec);
        sessionVocabRepository.save(sessionVocab);

        // Update session statistics
        switch (answer) {
            case CORRECT -> session.incrementCorrect();
            case WRONG -> session.incrementWrong();
            case SKIP -> session.incrementSkip();
        }
        
        // 🕒 Cộng dồn thời gian vào session
        if (timeSpentSec != null && timeSpentSec > 0) {
            Integer currentTime = session.getTimeSpentSec() != null ? session.getTimeSpentSec() : 0;
            session.setTimeSpentSec(currentTime + timeSpentSec);
        }
        
        session = sessionRepository.save(session);

        return session;
    }

    /**
     * Pause session
     */
    public LearningSession pauseSession(String sessionUuid) {
        LearningSession session = sessionRepository.findBySessionUuid(sessionUuid)
            .orElseThrow(() -> new RuntimeException("Session không tồn tại"));

        session.pause();
        session = sessionRepository.save(session);
        return session;
    }

    /**
     * Resume session
     */
    public LearningSession resumeSession(String sessionUuid) {
        LearningSession session = sessionRepository.findBySessionUuid(sessionUuid)
            .orElseThrow(() -> new RuntimeException("Session không tồn tại"));

        session.resume();
        session = sessionRepository.save(session);
        return session;
    }

    /**
     * Cancel session
     */
    public void cancelSession(String sessionUuid) {
        LearningSession session = sessionRepository.findBySessionUuid(sessionUuid)
            .orElseThrow(() -> new RuntimeException("Session không tồn tại"));

        session.setStatus(LearningSession.Status.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    /**
     * Complete session và batch update progress
     * Evict from cache khi complete
     */
    public SessionResultDTO completeSession(String sessionUuid, SessionResultRequest request) {
        LearningSession session = sessionRepository.findBySessionUuid(sessionUuid)
            .orElseThrow(() -> new RuntimeException("Session không tồn tại"));

        // ⚠️ KHÔNG xử lý lại answers ở đây vì frontend đã gọi recordAnswer() real-time
        // Chỉ cập nhật thời gian tổng từ request nếu có
        if (request != null && request.getDuration() != null && request.getDuration() > 0) {
            session.setTimeSpentSec(request.getDuration());
        }

        // Complete session
        session.complete();
        sessionRepository.save(session);

        // Batch update user progress (Option B - batch update)
        updateUserProgressFromSession(session);

        // Get session vocabularies for result
        List<SessionVocabulary> sessionVocabs = sessionVocabRepository.findBySessionOrderByOrderIndex(session);

        // Generate result DTO
        SessionResultDTO result = SessionResultDTO.fromSession(session, sessionVocabs);

        return result;
    }

    /**
     * Batch update user progress từ session (Option B)
     */
    private void updateUserProgressFromSession(LearningSession session) {
        List<SessionVocabulary> sessionVocabs = sessionVocabRepository
            .findBySessionOrderByOrderIndex(session);

        // Process theo batch để tránh memory issue
        for (int i = 0; i < sessionVocabs.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, sessionVocabs.size());
            List<SessionVocabulary> batch = sessionVocabs.subList(i, end);

            for (SessionVocabulary sv : batch) {
                if (sv.getUserAnswer() == null) continue; // Skip unanswered

                boolean correct = sv.getUserAnswer() == SessionVocabulary.AnswerType.CORRECT;
                userProgressService.updateProgress(
                    session.getUser(), 
                    sv.getVocab(), 
                    correct
                );
            }
        }
    }

    // ==================== SESSION QUERIES ====================

    /**
     * Lấy danh sách vocabularies trong session với pagination
     */
    public Page<SessionVocabulary> getSessionVocabularies(String sessionUuid, Pageable pageable) {
        LearningSession session = sessionRepository.findBySessionUuid(sessionUuid)
            .orElseThrow(() -> new RuntimeException("Session không tồn tại"));

        List<SessionVocabulary> allVocabs = sessionVocabRepository
            .findBySessionOrderByOrderIndex(session);

        // Eager load vocab and senses to avoid LazyInitializationException
        allVocabs.forEach(sv -> {
            if (sv.getVocab() != null) {
                sv.getVocab().getWord(); // trigger lazy load
                if (sv.getVocab().getSenses() != null) {
                    sv.getVocab().getSenses().size(); // trigger lazy load of senses
                }
            }
        });

        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allVocabs.size());
        
        List<SessionVocabulary> pageContent = start >= allVocabs.size() ? 
            new ArrayList<>() : allVocabs.subList(start, end);

        return new org.springframework.data.domain.PageImpl<>(
            pageContent, pageable, allVocabs.size());
    }

    /**
     * Lấy vocabularies chưa trả lời
     */
    public List<SessionVocabulary> getUnansweredVocabularies(String sessionUuid) {
        LearningSession session = sessionRepository.findBySessionUuid(sessionUuid)
            .orElseThrow(() -> new RuntimeException("Session không tồn tại"));

        return sessionVocabRepository.findUnansweredInSession(session);
    }

    /**
     * Lấy vocabularies trả lời sai
     */
    public List<SessionVocabulary> getWrongAnswers(String sessionUuid) {
        LearningSession session = sessionRepository.findBySessionUuid(sessionUuid)
            .orElseThrow(() -> new RuntimeException("Session không tồn tại"));

        return sessionVocabRepository.findWrongAnswersInSession(session);
    }

    /**
     * Lấy session statistics
     */
    public Map<String, Object> getSessionStatistics(String sessionUuid) {
        LearningSession session = sessionRepository.findBySessionUuid(sessionUuid)
            .orElseThrow(() -> new RuntimeException("Session không tồn tại"));

        int totalWords = Optional.ofNullable(session.getActualWords()).orElse(0);
        int correctCount = Optional.ofNullable(session.getCorrectCount()).orElse(0);
        int wrongCount = Optional.ofNullable(session.getWrongCount()).orElse(0);
        int skipCount = Optional.ofNullable(session.getSkipCount()).orElse(0);
        int timeSpent = Optional.ofNullable(session.getTimeSpentSec()).orElse(0);

        double accuracy = totalWords > 0
            ? (correctCount * 100.0) / totalWords
            : 0.0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("sessionUuid", session.getSessionUuid());
        stats.put("status", session.getStatus());
        stats.put("totalWords", totalWords);
        stats.put("correctCount", correctCount);
        stats.put("wrongCount", wrongCount);
        stats.put("skipCount", skipCount);
        stats.put("accuracy", accuracy);
        stats.put("accuracyRounded", Math.round(accuracy));
        stats.put("averageTimePerWord", totalWords > 0 ? (double) timeSpent / totalWords : 0.0);
        stats.put("timeSpent", timeSpent);
        stats.put("timeSpentFormatted", session.getFormattedDuration());
        stats.put("startedAt", session.getStartedAt());
        stats.put("completedAt", session.getCompletedAt());
        stats.put("expiresAt", session.getExpiresAt());
        stats.put("isExpired", session.isExpired());

        return stats;
    }

    @Transactional(readOnly = true)
    public SessionResultDTO getSessionResult(String sessionUuid) {
        LearningSession session = sessionRepository.findBySessionUuid(sessionUuid)
            .orElseThrow(() -> new RuntimeException("Session không tồn tại"));

        List<SessionVocabulary> sessionVocabs = sessionVocabRepository
            .findBySessionOrderByOrderIndex(session);

        return SessionResultDTO.fromSession(session, sessionVocabs);
    }

    // ==================== SCHEDULED TASKS ====================

    /**
     * Auto-complete expired sessions (Option A - auto save)
     * Chạy mỗi 5 phút
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void autoCompleteExpiredSessions() {
        List<LearningSession> expiredSessions = sessionRepository
            .findExpiredSessions(LocalDateTime.now());

        expiredSessions.forEach(session -> {
            try {
                session.complete();
                sessionRepository.save(session);
                updateUserProgressFromSession(session);
            } catch (Exception e) {
                log.error("Failed to auto-complete session {}: {}",
                    session.getSessionUuid(), e.getMessage());
            }
        });

        if (!expiredSessions.isEmpty()) {
            log.info("Auto-completed {} expired sessions", expiredSessions.size());
        }
    }

    /**
     * Cleanup old completed sessions
     * Chạy mỗi ngày lúc 2:00 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupOldSessions() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(SESSION_CLEANUP_DAYS);
        
        // Find old sessions
        List<LearningSession> oldSessions = sessionRepository.findAll().stream()
            .filter(s -> s.getStatus() == LearningSession.Status.COMPLETED)
            .filter(s -> s.getCompletedAt() != null && s.getCompletedAt().isBefore(cutoffDate))
            .collect(Collectors.toList());

        // Delete session vocabularies first (foreign key)
        for (LearningSession session : oldSessions) {
            sessionVocabRepository.deleteBySession(session);
        }

        // Delete sessions
        sessionRepository.deleteAll(oldSessions);

        if (!oldSessions.isEmpty()) {
            log.info("Cleaned up {} completed sessions older than {} days",
                oldSessions.size(), SESSION_CLEANUP_DAYS);
        }
    }

    // ==================== USER SESSION HISTORY ====================

    /**
     * Lấy lịch sử sessions của user
     */
    public Page<LearningSession> getUserSessionHistory(User user, Pageable pageable) {
        // Manual implementation vì repository chưa có method này
        List<LearningSession> allSessions = sessionRepository.findAll().stream()
            .filter(s -> s.getUser().equals(user))
            .sorted((s1, s2) -> s2.getStartedAt().compareTo(s1.getStartedAt()))
            .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allSessions.size());
        
        List<LearningSession> pageContent = start >= allSessions.size() ? 
            new ArrayList<>() : allSessions.subList(start, end);

        return new org.springframework.data.domain.PageImpl<>(
            pageContent, pageable, allSessions.size());
    }

    /**
     * Lấy session statistics của user
     */
    public Map<String, Object> getUserLearningStatistics(User user) {
        List<LearningSession> allSessions = sessionRepository.findAll().stream()
            .filter(s -> s.getUser().equals(user))
            .collect(Collectors.toList());

        long totalSessions = allSessions.size();
        long completedSessions = allSessions.stream()
            .filter(s -> s.getStatus() == LearningSession.Status.COMPLETED)
            .count();

        int totalVocabularies = allSessions.stream()
            .mapToInt(LearningSession::getTargetWords)
            .sum();

        int totalCorrect = allSessions.stream()
            .mapToInt(LearningSession::getCorrectCount)
            .sum();

        int totalWrong = allSessions.stream()
            .mapToInt(LearningSession::getWrongCount)
            .sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSessions", totalSessions);
        stats.put("completedSessions", completedSessions);
        stats.put("totalVocabularies", totalVocabularies);
        stats.put("totalCorrect", totalCorrect);
        stats.put("totalWrong", totalWrong);
        stats.put("overallAccuracy", totalVocabularies > 0 ?
            (double) totalCorrect / (totalCorrect + totalWrong) * 100 : 0.0);

        return stats;
    }
}
