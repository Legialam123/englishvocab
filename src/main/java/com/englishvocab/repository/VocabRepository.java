package com.englishvocab.repository;

import com.englishvocab.entity.Dictionary;
import com.englishvocab.entity.Vocab;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VocabRepository extends JpaRepository<Vocab, Integer> {
    
    /**
     * Tìm từ vựng theo từ điển
     */
    List<Vocab> findByDictionary(Dictionary dictionary);
    
    /**
     * Tìm từ vựng theo từ điển với phân trang
     */
    Page<Vocab> findByDictionary(Dictionary dictionary, Pageable pageable);
    
    /**
     * Tìm từ vựng theo dictionary, sắp xếp theo alphabet A-Z
     */
    List<Vocab> findByDictionaryOrderByWordAsc(Dictionary dictionary);
    
    /**
     * Tìm từ vựng theo dictionary với pagination, sắp xếp theo alphabet
     */
    Page<Vocab> findByDictionaryOrderByWordAsc(Dictionary dictionary, Pageable pageable);
    
    /**
     * Tìm từ vựng theo level
     */
    List<Vocab> findByLevel(Vocab.Level level);
    Page<Vocab> findByLevel(Vocab.Level level, Pageable pageable);
    
    /**
     * Tìm từ vựng theo từ điển và level
     */
    List<Vocab> findByDictionaryAndLevel(Dictionary dictionary, Vocab.Level level);
    Page<Vocab> findByDictionaryAndLevel(Dictionary dictionary, Vocab.Level level, Pageable pageable);
    
    /**
     * Tìm từ vựng theo word (search)
     */
    @Query("SELECT v FROM Vocab v WHERE LOWER(v.word) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Vocab> findByWordContainingIgnoreCase(@Param("keyword") String keyword);
    
    /**
     * Tìm từ vựng theo dictionary và word search
     */
    @Query("SELECT v FROM Vocab v WHERE v.dictionary = :dictionary AND LOWER(v.word) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Vocab> findByDictionaryAndWordContainingIgnoreCase(@Param("dictionary") Dictionary dictionary, @Param("keyword") String keyword);
    
    /**
     * Tìm từ vựng random theo dictionary (for quiz)
     */
    @Query(value = "SELECT * FROM vocab WHERE dictionary_id = :dictionaryId ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Vocab> findRandomVocabByDictionary(@Param("dictionaryId") Integer dictionaryId, @Param("limit") int limit);
    
    /**
     * Tìm từ vựng theo topic
     */
    @Query("SELECT v FROM Vocab v JOIN v.vocabTopics vt WHERE vt.topic.topicId = :topicId")
    List<Vocab> findByTopicId(@Param("topicId") Integer topicId);
    
    /**
     * Đếm từ vựng theo dictionary
     */
    long countByDictionary(Dictionary dictionary);
    
    /**
     * Đếm từ vựng theo level
     */
    long countByLevel(Vocab.Level level);
    
    /**
     * Đếm từ vựng theo dictionary và level
     */
    long countByDictionaryAndLevel(Dictionary dictionary, Vocab.Level level);
    
    /**
     * Tìm từ vựng theo exact word
     */
    Optional<Vocab> findByDictionaryAndWord(Dictionary dictionary, String word);
    
    /**
     * Check existence by dictionary and word
     */
    boolean existsByDictionaryAndWord(Dictionary dictionary, String word);
    
    /**
     * Tìm từ vựng theo IPA
     */
    List<Vocab> findByIpaContainingIgnoreCase(String ipa);
    
    /**
     * Tìm từ vựng theo dictionary và word starting with letter
     */
    @Query("SELECT v FROM Vocab v WHERE v.dictionary = :dictionary AND LOWER(v.word) LIKE LOWER(CONCAT(:startLetter, '%'))")
    List<Vocab> findByDictionaryAndWordStartingWith(@Param("dictionary") Dictionary dictionary, @Param("startLetter") String startLetter);
    
    /**
     * Tìm từ vựng theo dictionary và word starting with letter (pagination)
     */
    @Query("SELECT v FROM Vocab v WHERE v.dictionary = :dictionary AND LOWER(v.word) LIKE LOWER(CONCAT(:startLetter, '%'))")
    Page<Vocab> findByDictionaryAndWordStartingWith(@Param("dictionary") Dictionary dictionary, @Param("startLetter") String startLetter, Pageable pageable);
    
    /**
     * Tìm từ vựng theo dictionary và word containing (pagination)
     */
    @Query("SELECT v FROM Vocab v WHERE v.dictionary = :dictionary AND LOWER(v.word) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Vocab> findByDictionaryAndWordContainingIgnoreCase(@Param("dictionary") Dictionary dictionary, @Param("keyword") String keyword, Pageable pageable);

    /**
     * Tìm từ vựng theo dictionary và word bắt đầu bằng tập chữ cái (pagination)
     */
    @Query("SELECT v FROM Vocab v WHERE v.dictionary = :dictionary AND LOWER(SUBSTRING(v.word, 1, 1)) IN :startLetters ORDER BY LOWER(v.word)")
    Page<Vocab> findByDictionaryAndWordStartingWithIn(@Param("dictionary") Dictionary dictionary,
                                                      @Param("startLetters") List<String> startLetters,
                                                      Pageable pageable);

    /**
     * Đếm số lượng từ vựng theo chữ cái đầu tiên trong dictionary
     */
    @Query("SELECT LOWER(SUBSTRING(v.word, 1, 1)) AS prefix, COUNT(v) FROM Vocab v WHERE v.dictionary = :dictionary GROUP BY LOWER(SUBSTRING(v.word, 1, 1))")
    List<Object[]> countByDictionaryGroupedByFirstLetter(@Param("dictionary") Dictionary dictionary);
}