package com.englishvocab.service;

import com.englishvocab.dto.SessionResultDTO;
import com.englishvocab.dto.SessionResultRequest;
import com.englishvocab.entity.*;
import com.englishvocab.repository.LearningSessionRepository;
import com.englishvocab.repository.SessionVocabularyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service quản lý Learning Sessions với Redis caching
 * - Active sessions: cached trong Redis (30min TTL)
 * - Auto-expire sau 30 phút
 * - Scheduled cleanup jobs
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
     * Session được cache trong Redis với TTL 30 phút
     */
    @CachePut(value = "active_sessions", key = "#result.sessionUuid")
    public LearningSession createSession(User user, com.englishvocab.entity.Dictionary dictionary, 
                                        String learningMode, List<Integer> vocabularyIds, 
                                        Integer maxVocabularies) {
        // Kiểm tra session active
        if (hasActiveSession(user)) {
            throw new RuntimeException("Bạn đã có phiên học đang hoạt động. Vui lòng hoàn thành hoặc tạm dừng trước!");
        }

        // Select vocabularies theo priority
        List<Vocab> vocabularies = selectVocabulariesForSession(
            user, dictionary, learningMode, vocabularyIds, maxVocabularies);

        if (vocabularies.isEmpty()) {
            throw new RuntimeException("Không tìm thấy từ vựng phù hợp!");
        }

        // Create session với timeout 30 phút
        LearningSession session = new LearningSession();
        session.setUser(user);
        session.setDictionary(dictionary);
        session.setSessionUuid(UUID.randomUUID().toString());
        session.setStatus(LearningSession.Status.ACTIVE);
        session.setLearningMode(parseLearningMode(learningMode));
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

        log.info("Created session {} for user {} with {} vocabularies, expires at {}",
            session.getSessionUuid(), user.getEmail(), vocabularies.size(), session.getExpiresAt());

        return session;
    }

    /**
     * Parse learning mode string to enum
     */
    private LearningSession.LearningMode parseLearningMode(String mode) {
        return switch (mode.toLowerCase()) {
            case "alphabetical" -> LearningSession.LearningMode.ALPHABETICAL;
            case "topics" -> LearningSession.LearningMode.TOPICS;
            case "custom" -> LearningSession.LearningMode.CUSTOM;
            case "review" -> LearningSession.LearningMode.REVIEW;
            default -> LearningSession.LearningMode.REVIEW;
        };
    }

    /**
     * Select vocabularies với priority: Review > New > Random
     */
    private List<Vocab> selectVocabulariesForSession(User user, com.englishvocab.entity.Dictionary dictionary, 
                                                     String learningMode, List<Integer> vocabularyIds, 
                                                     Integer maxVocabularies) {
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
                // A-Z order
                selected = vocabularyService.getVocabulariesByDictionary(dictionary, maxVocabularies);
                break;

            case "random":
            default:
                // Random
                selected = vocabularyService.getRandomVocabularies(dictionary, maxVocabularies);
                break;
        }

        // Nếu không đủ, fill với từ random
        if (selected.size() < maxVocabularies) {
            List<Vocab> additional = vocabularyService.getRandomVocabularies(
                dictionary, maxVocabularies - selected.size());
            selected.addAll(additional);
        }

        // Shuffle để tránh predictable
        Collections.shuffle(selected);

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
     * Cached trong Redis để fast access
     */
    @Cacheable(value = "active_sessions", key = "#sessionUuid")
    public Optional<LearningSession> getActiveSession(User user) {
        List<LearningSession> sessions = sessionRepository.findActiveOrPausedSessions(user);
        return sessions.isEmpty() ? Optional.empty() : Optional.of(sessions.get(0));
    }
    
    /**
     * Get session by UUID (with caching)
     */
    @Cacheable(value = "active_sessions", key = "#sessionUuid")
    public LearningSession getSessionByUuid(String sessionUuid) {
        return sessionRepository.findBySessionUuid(sessionUuid)
            .orElseThrow(() -> new RuntimeException("Session không tồn tại"));
    }

    // ==================== SESSION ACTIONS ====================

    /**
     * Record câu trả lời của user
     * Update cache sau khi record
     */
    @CachePut(value = "active_sessions", key = "#sessionUuid")
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
        session = sessionRepository.save(session);

        log.debug("Recorded answer {} for vocab {} in session {}", 
            answer, vocabularyId, sessionUuid);
            
        return session;
    }

    /**
     * Pause session (update cache)
     */
    @CachePut(value = "active_sessions", key = "#sessionUuid")
    public LearningSession pauseSession(String sessionUuid) {
        LearningSession session = sessionRepository.findBySessionUuid(sessionUuid)
            .orElseThrow(() -> new RuntimeException("Session không tồn tại"));

        session.pause();
        session = sessionRepository.save(session);

        log.info("Paused session {}", sessionUuid);
        return session;
    }

    /**
     * Resume session (update cache)
     */
    @CachePut(value = "active_sessions", key = "#sessionUuid")
    public LearningSession resumeSession(String sessionUuid) {
        LearningSession session = sessionRepository.findBySessionUuid(sessionUuid)
            .orElseThrow(() -> new RuntimeException("Session không tồn tại"));

        session.resume();
        session = sessionRepository.save(session);

        log.info("Resumed session {}", sessionUuid);
        return session;
    }

    /**
     * Cancel session (evict from cache)
     */
    @CacheEvict(value = "active_sessions", key = "#sessionUuid")
    public void cancelSession(String sessionUuid) {
        LearningSession session = sessionRepository.findBySessionUuid(sessionUuid)
            .orElseThrow(() -> new RuntimeException("Session không tồn tại"));

        session.setStatus(LearningSession.Status.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());
        sessionRepository.save(session);

        log.info("Cancelled session {}", sessionUuid);
    }

    /**
     * Complete session và batch update progress
     * Evict from cache khi complete
     */
    @CacheEvict(value = "active_sessions", key = "#sessionUuid")
    public SessionResultDTO completeSession(String sessionUuid, SessionResultRequest request) {
        LearningSession session = sessionRepository.findBySessionUuid(sessionUuid)
            .orElseThrow(() -> new RuntimeException("Session không tồn tại"));

        // Process remaining answers từ request
        if (request != null && request.getAnswers() != null) {
            for (SessionResultRequest.VocabAnswer answer : request.getAnswers()) {
                SessionVocabulary.AnswerType answerType = SessionVocabulary.AnswerType.valueOf(answer.getAnswerType());
                recordAnswer(sessionUuid, answer.getVocabId(), 
                           answerType, answer.getTimeSpent());
            }
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

        log.info("Completed session {} - Result: {}/{} correct", 
            sessionUuid, session.getCorrectCount(), session.getTargetWords());

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

        log.info("Batch updated progress for {} vocabularies in session {}", 
            sessionVocabs.size(), session.getSessionUuid());
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

        long answered = sessionVocabRepository.countAnsweredInSession(session);
        long unanswered = session.getTargetWords() - answered;

        Map<String, Object> stats = new HashMap<>();
        stats.put("sessionUuid", session.getSessionUuid());
        stats.put("status", session.getStatus());
        stats.put("total", session.getTargetWords());
        stats.put("answered", answered);
        stats.put("unanswered", unanswered);
        stats.put("correct", session.getCorrectCount());
        stats.put("wrong", session.getWrongCount());
        stats.put("skip", session.getSkipCount());
        stats.put("accuracy", answered > 0 ? 
            (double) session.getCorrectCount() / answered * 100 : 0.0);
        stats.put("startedAt", session.getStartedAt());
        stats.put("expiresAt", session.getExpiresAt());
        stats.put("isExpired", session.isExpired());

        return stats;
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

        for (LearningSession session : expiredSessions) {
            try {
                log.info("Auto-completing expired session {}", session.getSessionUuid());
                
                // Complete session
                session.complete();
                sessionRepository.save(session);

                // Batch update progress
                updateUserProgressFromSession(session);

            } catch (Exception e) {
                log.error("Failed to auto-complete session {}: {}", 
                    session.getSessionUuid(), e.getMessage());
            }
        }

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

        log.info("Cleaned up {} old sessions older than {} days", 
            oldSessions.size(), SESSION_CLEANUP_DAYS);
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
