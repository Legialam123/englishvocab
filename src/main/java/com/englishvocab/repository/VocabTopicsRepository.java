package com.englishvocab.repository;

import com.englishvocab.entity.Dictionary;
import com.englishvocab.entity.VocabTopics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface VocabTopicsRepository extends JpaRepository<VocabTopics, VocabTopics.VocabTopicId> {
    
    /**
     * Tìm tất cả topics của một vocab
     */
    List<VocabTopics> findByVocabId(Integer vocabId);
    
    /**
     * Tìm tất cả vocab của một topic
     */
    List<VocabTopics> findByTopicId(Integer topicId);
    
    /**
     * Kiểm tra xem vocab có thuộc topic không
     */
    boolean existsByVocabIdAndTopicId(Integer vocabId, Integer topicId);
    
    /**
     * Đếm số topics của một vocab
     */
    @Query("SELECT COUNT(vt) FROM VocabTopics vt WHERE vt.vocabId = :vocabId")
    long countByVocabId(@Param("vocabId") Integer vocabId);
    
    /**
     * Đếm số vocab của một topic
     */
    @Query("SELECT COUNT(vt) FROM VocabTopics vt WHERE vt.topicId = :topicId")
    long countByTopicId(@Param("topicId") Integer topicId);
    
    /**
     * Xóa tất cả topics của một vocab
     */
    @Modifying
    @Transactional
    void deleteByVocabId(Integer vocabId);
    
    /**
     * Xóa tất cả vocab của một topic
     */
    @Modifying
    @Transactional
    void deleteByTopicId(Integer topicId);
    
    /**
     * Xóa một relation cụ thể
     */
    @Modifying
    @Transactional
    void deleteByVocabIdAndTopicId(Integer vocabId, Integer topicId);
    
    /**
     * Đếm số vocab của một topic trong dictionary cụ thể
     */
    @Query("SELECT COUNT(vt) FROM VocabTopics vt JOIN vt.vocab v WHERE vt.topicId = :topicId AND v.dictionary = :dictionary")
    long countByTopicIdAndVocabDictionary(@Param("topicId") Integer topicId, @Param("dictionary") Dictionary dictionary);
}
