package com.englishvocab.repository;

import com.englishvocab.entity.LearningSession;
import com.englishvocab.entity.SessionVocabulary;
import com.englishvocab.entity.Vocab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionVocabularyRepository extends JpaRepository<SessionVocabulary, Long> {
    
    /**
     * Tìm tất cả từ vựng trong session (theo thứ tự)
     */
    List<SessionVocabulary> findBySessionOrderByOrderIndex(LearningSession session);
    
    /**
     * Tìm từ vựng cụ thể trong session
     */
    Optional<SessionVocabulary> findBySessionAndVocab(LearningSession session, Vocab vocab);
    
    /**
     * Tìm từ vựng theo thứ tự index
     */
    Optional<SessionVocabulary> findBySessionAndOrderIndex(LearningSession session, Integer orderIndex);
    
    /**
     * Đếm từ đã trả lời trong session
     */
    @Query("SELECT COUNT(sv) FROM SessionVocabulary sv WHERE sv.session = :session AND sv.userAnswer IS NOT NULL")
    long countAnsweredInSession(@Param("session") LearningSession session);
    
    /**
     * Đếm từ chưa trả lời trong session
     */
    @Query("SELECT COUNT(sv) FROM SessionVocabulary sv WHERE sv.session = :session AND sv.userAnswer IS NULL")
    long countUnansweredInSession(@Param("session") LearningSession session);
    
    /**
     * Lấy từ tiếp theo chưa trả lời
     */
    @Query("SELECT sv FROM SessionVocabulary sv WHERE sv.session = :session AND sv.userAnswer IS NULL ORDER BY sv.orderIndex ASC")
    List<SessionVocabulary> findUnansweredInSession(@Param("session") LearningSession session);
    
    /**
     * Lấy từ đã trả lời sai
     */
    @Query("SELECT sv FROM SessionVocabulary sv WHERE sv.session = :session AND sv.userAnswer = 'WRONG' ORDER BY sv.orderIndex ASC")
    List<SessionVocabulary> findWrongAnswersInSession(@Param("session") LearningSession session);
    
    /**
     * Lấy từ đã trả lời đúng
     */
    @Query("SELECT sv FROM SessionVocabulary sv WHERE sv.session = :session AND sv.userAnswer = 'CORRECT' ORDER BY sv.orderIndex ASC")
    List<SessionVocabulary> findCorrectAnswersInSession(@Param("session") LearningSession session);
    
    /**
     * Xóa tất cả vocabulary của session
     */
    void deleteBySession(LearningSession session);
}
