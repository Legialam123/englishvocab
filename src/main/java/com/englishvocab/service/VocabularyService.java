package com.englishvocab.service;

import com.englishvocab.entity.Vocab;
import com.englishvocab.entity.Dictionary;
import com.englishvocab.entity.Senses;
import com.englishvocab.entity.Topics;
import com.englishvocab.entity.VocabTopics;
import com.englishvocab.repository.VocabRepository;
import com.englishvocab.repository.DictionaryRepository;
import com.englishvocab.repository.SensesRepository;
import com.englishvocab.repository.VocabTopicsRepository;
import com.englishvocab.repository.TopicsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VocabularyService {
    
    private final VocabRepository vocabRepository;
    private final DictionaryRepository dictionaryRepository;
    private final SensesRepository sensesRepository;
    private final VocabTopicsRepository vocabTopicsRepository;
    private final TopicsRepository topicsRepository;
    
    /**
     * Lấy tất cả từ vựng
     */
    public List<Vocab> findAll() {
        return vocabRepository.findAll();
    }
    
    /**
     * Lấy từ vựng theo ID
     */
    public Optional<Vocab> findById(Integer id) {
        return vocabRepository.findById(id);
    }
    
    /**
     * Lấy từ vựng theo ID hoặc throw exception
     */
    public Vocab findByIdOrThrow(Integer id) {
        return vocabRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy từ vựng với ID: " + id));
    }
    
    /**
     * Lấy từ vựng theo dictionary với phân trang
     */
    public Page<Vocab> findByDictionary(Integer dictionaryId, Pageable pageable) {
        Dictionary dictionary = dictionaryRepository.findById(dictionaryId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy từ điển với ID: " + dictionaryId));
        return vocabRepository.findByDictionary(dictionary, pageable);
    }
    
    /**
     * Lấy từ vựng với phân trang và filter
     */
    public Page<Vocab> findAllWithFilter(Integer dictionaryId, Vocab.Level level, Pageable pageable) {
        if (dictionaryId != null && level != null) {
            Dictionary dictionary = dictionaryRepository.findById(dictionaryId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy từ điển với ID: " + dictionaryId));
            return vocabRepository.findByDictionaryAndLevel(dictionary, level, pageable);
        } else if (dictionaryId != null) {
            Dictionary dictionary = dictionaryRepository.findById(dictionaryId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy từ điển với ID: " + dictionaryId));
            return vocabRepository.findByDictionary(dictionary, pageable);
        } else if (level != null) {
            return vocabRepository.findByLevel(level, pageable);
        } else {
            return vocabRepository.findAll(pageable);
        }
    }
    
    /**
     * Tìm kiếm từ vựng theo word
     */
    public List<Vocab> searchByWord(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        return vocabRepository.findByWordContainingIgnoreCase(keyword.trim());
    }
    
    /**
     * Tìm kiếm từ vựng theo word trong dictionary
     */
    public List<Vocab> searchByWordInDictionary(Integer dictionaryId, String keyword) {
        Dictionary dictionary = dictionaryRepository.findById(dictionaryId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy từ điển với ID: " + dictionaryId));
        
        if (keyword == null || keyword.trim().isEmpty()) {
            return vocabRepository.findByDictionary(dictionary);
        }
        return vocabRepository.findByDictionaryAndWordContainingIgnoreCase(dictionary, keyword.trim());
    }
    
    /**
     * Tạo từ vựng mới
     */
    public Vocab create(Vocab vocab) {
        log.info("Tạo từ vựng mới: {}", vocab.getWord());
        
        // Validate business rules
        validateVocab(vocab);
        
        // Check dictionary exists and is active
        Dictionary dictionary = dictionaryRepository.findById(vocab.getDictionary().getDictionaryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy từ điển với ID: " + vocab.getDictionary().getDictionaryId()));
        
        if (dictionary.getStatus() != Dictionary.Status.ACTIVE) {
            throw new RuntimeException("Không thể thêm từ vào từ điển không hoạt động");
        }
        
        vocab.setDictionary(dictionary);
        
        // Check duplicate word in same dictionary
        if (vocabRepository.existsByDictionaryAndWord(dictionary, vocab.getWord())) {
            throw new RuntimeException("Từ '" + vocab.getWord() + "' đã tồn tại trong từ điển này");
        }
        
        Vocab saved = vocabRepository.save(vocab);
        log.info("Đã tạo từ vựng với ID: {}", saved.getVocabId());
        return saved;
    }
    
    /**
     * Tạo từ vựng với nghĩa chính
     */
    public Vocab createWithSense(Vocab vocab, Senses primarySense) {
        log.info("Tạo từ vựng với nghĩa: {} -> {}", vocab.getWord(), primarySense.getMeaningVi());
        
        // Create vocab first
        Vocab saved = create(vocab);
        
        // Set the vocab reference for the sense
        primarySense.setVocab(saved);
        
        // Validate sense
        if (primarySense.getMeaningVi() == null || primarySense.getMeaningVi().trim().isEmpty()) {
            throw new RuntimeException("Nghĩa tiếng Việt không được để trống");
        }
        
        if (primarySense.getMeaningVi().length() > 50) {
            throw new RuntimeException("Nghĩa tiếng Việt không được vượt quá 50 ký tự");
        }
        
        if (primarySense.getDefinition() != null && primarySense.getDefinition().length() > 100) {
            throw new RuntimeException("Định nghĩa không được vượt quá 100 ký tự");
        }
        
        // Save the primary sense
        Senses savedSense = sensesRepository.save(primarySense);
        log.info("Đã tạo nghĩa chính với ID: {} cho từ vựng: {}", savedSense.getSenseId(), saved.getWord());
        
        return saved;
    }
    
    /**
     * Cập nhật từ vựng
     */
    public Vocab update(Integer id, Vocab updatedVocab) {
        log.info("Cập nhật từ vựng ID: {}", id);
        
        Vocab existing = findByIdOrThrow(id);
        
        // Validate business rules
        validateVocab(updatedVocab);
        
        // Check dictionary exists
        Dictionary dictionary = dictionaryRepository.findById(updatedVocab.getDictionary().getDictionaryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy từ điển với ID: " + updatedVocab.getDictionary().getDictionaryId()));
        
        // Check duplicate word in same dictionary (exclude current record)
        vocabRepository.findByDictionaryAndWord(dictionary, updatedVocab.getWord())
                .ifPresent(vocab -> {
                    if (!vocab.getVocabId().equals(id)) {
                        throw new RuntimeException("Từ '" + updatedVocab.getWord() + "' đã tồn tại trong từ điển này");
                    }
                });
        
        // Update fields
        existing.setWord(updatedVocab.getWord());
        existing.setIpa(updatedVocab.getIpa());
        existing.setLevel(updatedVocab.getLevel());
        existing.setDictionary(dictionary);
        
        Vocab saved = vocabRepository.save(existing);
        log.info("Đã cập nhật từ vựng: {}", saved.getWord());
        return saved;
    }
    
    /**
     * Xóa từ vựng
     */
    public void delete(Integer id) {
        log.info("Xóa từ vựng ID: {}", id);
        
        Vocab vocab = findByIdOrThrow(id);
        
        // Check if vocab has user progress
        // TODO: Implement check for UserVocabProgress
        
        // Delete associated senses first
        List<Senses> senses = sensesRepository.findByVocab(vocab);
        if (!senses.isEmpty()) {
            log.info("Xóa {} nghĩa của từ: {}", senses.size(), vocab.getWord());
            sensesRepository.deleteAll(senses);
        }
        
        vocabRepository.delete(vocab);
        log.info("Đã xóa từ vựng: {}", vocab.getWord());
    }
    
    // ==================== HELPER METHODS FOR LEARNING SERVICE ====================
    
    /**
     * Lấy danh sách vocab theo IDs
     */
    public List<Vocab> findByIdIn(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return vocabRepository.findAllById(ids);
    }
    
    /**
     * Lấy vocabularies từ dictionary (giới hạn số lượng)
     */
    public List<Vocab> getVocabulariesByDictionary(com.englishvocab.entity.Dictionary dictionary, Integer limit) {
        List<Vocab> allVocabs = vocabRepository.findByDictionary(dictionary);
        
        if (limit != null && limit > 0 && allVocabs.size() > limit) {
            return allVocabs.subList(0, limit);
        }
        
        return allVocabs;
    }
    
    /**
     * Lấy random vocabularies từ dictionary
     */
    public List<Vocab> getRandomVocabularies(com.englishvocab.entity.Dictionary dictionary, Integer limit) {
        List<Vocab> allVocabs = vocabRepository.findByDictionary(dictionary);
        
        // Shuffle để random
        java.util.Collections.shuffle(allVocabs);
        
        if (limit != null && limit > 0 && allVocabs.size() > limit) {
            return allVocabs.subList(0, limit);
        }
        
        return allVocabs;
    }
    
    /**
     * Lấy vocabularies từ dictionary theo level
     */
    public List<Vocab> getVocabulariesByDictionaryAndLevel(com.englishvocab.entity.Dictionary dictionary, 
                                                           String level, Integer limit) {
        Vocab.Level vocabLevel = Vocab.Level.valueOf(level.toUpperCase());
        List<Vocab> allVocabs = vocabRepository.findByDictionaryAndLevel(dictionary, vocabLevel);
        
        // Sort by word alphabetically for alphabetical mode
        allVocabs.sort(java.util.Comparator.comparing(Vocab::getWord));
        
        if (limit != null && limit > 0 && allVocabs.size() > limit) {
            return allVocabs.subList(0, limit);
        }
        
        return allVocabs;
    }
    
    /**
     * Lấy vocabularies từ dictionary theo startLetter
     */
    public List<Vocab> getVocabulariesByDictionaryAndLetter(com.englishvocab.entity.Dictionary dictionary, 
                                                            String startLetter, Integer limit) {
        List<Vocab> allVocabs = vocabRepository.findByDictionaryOrderByWordAsc(dictionary);
        
        // Filter by first letter
        String lowerLetter = startLetter.toLowerCase();
        List<Vocab> filtered = allVocabs.stream()
            .filter(v -> v.getWord().toLowerCase().startsWith(lowerLetter))
            .collect(java.util.stream.Collectors.toList());
        
        if (limit != null && limit > 0 && filtered.size() > limit) {
            return filtered.subList(0, limit);
        }
        
        return filtered;
    }
    
    /**
     * Lấy vocabularies từ dictionary theo level và startLetter
     */
    public List<Vocab> getVocabulariesByDictionaryAndLevelAndLetter(com.englishvocab.entity.Dictionary dictionary, 
                                                                    String level, String startLetter, Integer limit) {
        Vocab.Level vocabLevel = Vocab.Level.valueOf(level.toUpperCase());
        List<Vocab> allVocabs = vocabRepository.findByDictionaryAndLevel(dictionary, vocabLevel);
        
        // Filter by first letter
        String lowerLetter = startLetter.toLowerCase();
        List<Vocab> filtered = allVocabs.stream()
            .filter(v -> v.getWord().toLowerCase().startsWith(lowerLetter))
            .sorted(java.util.Comparator.comparing(Vocab::getWord))
            .collect(java.util.stream.Collectors.toList());
        
        if (limit != null && limit > 0 && filtered.size() > limit) {
            return filtered.subList(0, limit);
        }
        
        return filtered;
    }
    
    /**
     * Get vocabularies by dictionary and topics (with optional level filter)
     */
    public List<Vocab> getVocabulariesByDictionaryAndTopics(com.englishvocab.entity.Dictionary dictionary, 
                                                            List<Integer> topicIds, String level, Integer limit) {
        List<Vocab> allVocabs;
        
        if (level != null && !level.isEmpty()) {
            // With level filter
            Vocab.Level vocabLevel = Vocab.Level.valueOf(level.toUpperCase());
            allVocabs = vocabRepository.findByDictionaryAndTopicsIdInAndLevel(dictionary, topicIds, vocabLevel);
        } else {
            // Without level filter
            allVocabs = vocabRepository.findByDictionaryAndTopicsIdIn(dictionary, topicIds);
        }
        
        // Sort by word alphabetically
        allVocabs.sort(Comparator.comparing(Vocab::getWord));
        
        // Limit results
        return allVocabs.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * Thống kê từ vựng
     */
    public VocabStats getStatistics() {
        long total = vocabRepository.count();
        long beginner = vocabRepository.countByLevel(Vocab.Level.BEGINNER);
        long intermediate = vocabRepository.countByLevel(Vocab.Level.INTERMEDIATE);
        long advanced = vocabRepository.countByLevel(Vocab.Level.ADVANCED);
        
        return VocabStats.builder()
                .total(total)
                .beginner(beginner)
                .intermediate(intermediate)
                .advanced(advanced)
                .build();
    }
    
    /**
     * Thống kê từ vựng theo dictionary
     */
    public VocabStatsByDictionary getStatisticsByDictionary(Integer dictionaryId) {
        Dictionary dictionary = dictionaryRepository.findById(dictionaryId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy từ điển với ID: " + dictionaryId));
        
        long total = vocabRepository.countByDictionary(dictionary);
        long beginner = vocabRepository.countByDictionaryAndLevel(dictionary, Vocab.Level.BEGINNER);
        long intermediate = vocabRepository.countByDictionaryAndLevel(dictionary, Vocab.Level.INTERMEDIATE);
        long advanced = vocabRepository.countByDictionaryAndLevel(dictionary, Vocab.Level.ADVANCED);
        
        return VocabStatsByDictionary.builder()
                .dictionary(dictionary)
                .total(total)
                .beginner(beginner)
                .intermediate(intermediate)
                .advanced(advanced)
                .build();
    }

    public int countByDictionary(Integer dictionaryId) {
        Dictionary dictionary = dictionaryRepository.findById(dictionaryId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy từ điển với ID: " + dictionaryId));
        return (int) vocabRepository.countByDictionary(dictionary);
    }
    
    /**
     * Lấy danh sách từ điển có từ vựng
     */
    public List<Dictionary> getDictionariesWithVocab() {
        return dictionaryRepository.findDictionariesWithVocabulary();
    }
    
    /**
     * Validate vocabulary
     */
    private void validateVocab(Vocab vocab) {
        if (vocab.getWord() == null || vocab.getWord().trim().isEmpty()) {
            throw new RuntimeException("Từ vựng không được để trống");
        }
        
        if (vocab.getWord().length() > 100) {
            throw new RuntimeException("Từ vựng không được vượt quá 100 ký tự");
        }
        
        if (vocab.getIpa() != null && vocab.getIpa().length() > 200) {
            throw new RuntimeException("Phiên âm IPA không được vượt quá 200 ký tự");
        }
        
        if (vocab.getDictionary() == null || vocab.getDictionary().getDictionaryId() == null) {
            throw new RuntimeException("Phải chỉ định từ điển");
        }
        
        if (vocab.getLevel() == null) {
            throw new RuntimeException("Phải chỉ định level của từ vựng");
        }
    }
    
    /**
     * Vocabulary Statistics DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class VocabStats {
        private long total;
        private long beginner;
        private long intermediate;
        private long advanced;
    }
    
    /**
     * Vocabulary Statistics by Dictionary DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class VocabStatsByDictionary {
        private Dictionary dictionary;
        private long total;
        private long beginner;
        private long intermediate;
        private long advanced;
    }
    
    // ===== TOPIC MANAGEMENT METHODS =====
    
    /**
     * Assign topics to vocabulary
     */
    public void assignTopics(Integer vocabId, List<Integer> topicIds) {
        log.info("Assigning {} topics to vocab ID: {}", topicIds.size(), vocabId);
        
        // Validate vocab exists
        Vocab vocab = findByIdOrThrow(vocabId);
        
        // Clear existing topics first
        vocabTopicsRepository.deleteByVocabId(vocabId);
        
        // Assign new topics
        for (Integer topicId : topicIds) {
            // Validate topic exists and is active
            Topics topic = topicsRepository.findById(topicId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chủ đề với ID: " + topicId));
                    
            if (topic.getStatus() != Topics.Status.ACTIVE) {
                log.warn("Skipping inactive topic: {}", topic.getName());
                continue;
            }
            
            VocabTopics vocabTopic = VocabTopics.builder()
                    .vocabId(vocabId)
                    .topicId(topicId)
                    .build();
                    
            vocabTopicsRepository.save(vocabTopic);
            log.debug("Assigned topic '{}' to vocab '{}'", topic.getName(), vocab.getWord());
        }
        
        log.info("Successfully assigned {} topics to vocab: {}", topicIds.size(), vocab.getWord());
    }
    
    /**
     * Get topics for vocabulary
     */
    public List<Topics> getTopicsForVocab(Integer vocabId) {
        List<VocabTopics> vocabTopics = vocabTopicsRepository.findByVocabId(vocabId);
        return vocabTopics.stream()
                .map(vt -> vt.getTopic())
                .filter(topic -> topic != null)
                .toList();
    }
    
    /**
     * Remove topic from vocabulary
     */
    public void removeTopicFromVocab(Integer vocabId, Integer topicId) {
        vocabTopicsRepository.deleteByVocabIdAndTopicId(vocabId, topicId);
        log.info("Removed topic {} from vocab {}", topicId, vocabId);
    }
    
    /**
     * Check if vocabulary has topic
     */
    public boolean hasTopicAssigned(Integer vocabId, Integer topicId) {
        return vocabTopicsRepository.existsByVocabIdAndTopicId(vocabId, topicId);
    }
    
    // ===== LEARNING MODE METHODS =====
    
    /**
     * Find vocabulary by dictionary with alphabetical ordering
     */
    public Page<Vocab> findByDictionaryOrderByWordAsc(Integer dictionaryId, Pageable pageable) {
        Dictionary dictionary = dictionaryRepository.findById(dictionaryId)
                .orElseThrow(() -> new RuntimeException("Dictionary not found: " + dictionaryId));
        return vocabRepository.findByDictionaryOrderByWordAsc(dictionary, pageable);
    }
    
    /**
     * Find vocabulary by dictionary and word starting with letter
     */
    public Page<Vocab> findByDictionaryAndWordStartingWith(Integer dictionaryId, String startLetter, Pageable pageable) {
        Dictionary dictionary = dictionaryRepository.findById(dictionaryId)
                .orElseThrow(() -> new RuntimeException("Dictionary not found: " + dictionaryId));
        return vocabRepository.findByDictionaryAndWordStartingWith(dictionary, startLetter, pageable);
    }

    /**
     * Find vocabulary by dictionary and word starting with any of the provided letters
     */
    public Page<Vocab> findByDictionaryAndWordStartingWith(Integer dictionaryId, Collection<String> startLetters,
                                                           Pageable pageable) {
        Dictionary dictionary = dictionaryRepository.findById(dictionaryId)
                .orElseThrow(() -> new RuntimeException("Dictionary not found: " + dictionaryId));

    List<String> normalizedLetters = startLetters.stream()
        .filter(letter -> letter != null && !letter.isBlank())
        .map(letter -> letter.substring(0, 1).toLowerCase(Locale.ROOT))
        .distinct()
        .sorted()
        .collect(Collectors.toList());

        if (normalizedLetters.isEmpty()) {
            return Page.empty(pageable);
        }

        return vocabRepository.findByDictionaryAndWordStartingWithIn(dictionary, normalizedLetters, pageable);
    }
    
    /**
     * Find vocabulary by dictionary and level
     */
    public Page<Vocab> findByDictionaryAndLevel(Integer dictionaryId, String level, Pageable pageable) {
        Dictionary dictionary = dictionaryRepository.findById(dictionaryId)
                .orElseThrow(() -> new RuntimeException("Dictionary not found: " + dictionaryId));
        Vocab.Level vocabLevel = Vocab.Level.valueOf(level.toUpperCase());
        return vocabRepository.findByDictionaryAndLevel(dictionary, vocabLevel, pageable);
    }
    
    /**
     * Count vocabulary by dictionary and level
     */
    public long countByDictionaryAndLevel(Integer dictionaryId, String level) {
        Dictionary dictionary = dictionaryRepository.findById(dictionaryId)
                .orElseThrow(() -> new RuntimeException("Dictionary not found: " + dictionaryId));
        Vocab.Level vocabLevel = Vocab.Level.valueOf(level.toUpperCase());
        return vocabRepository.countByDictionaryAndLevel(dictionary, vocabLevel);
    }
    
    /**
     * Count vocabulary by dictionary and word starting with letters
     */
    public long countByDictionaryAndWordStartingWith(Integer dictionaryId, List<String> letters) {
        Dictionary dictionary = dictionaryRepository.findById(dictionaryId)
                .orElseThrow(() -> new RuntimeException("Dictionary not found: " + dictionaryId));
        
        return letters.stream()
                .mapToLong(letter -> vocabRepository.countByDictionaryAndWordStartingWithIgnoreCase(dictionary, letter))
                .sum();
    }
    
    /**
     * Find vocabulary by dictionary, word starting with letters, and level
     */
    public Page<Vocab> findByDictionaryAndWordStartingWithAndLevel(Integer dictionaryId, List<String> letters, 
                                                                    String level, Pageable pageable) {
        Dictionary dictionary = dictionaryRepository.findById(dictionaryId)
                .orElseThrow(() -> new RuntimeException("Dictionary not found: " + dictionaryId));
        Vocab.Level vocabLevel = Vocab.Level.valueOf(level.toUpperCase());
        
        return vocabRepository.findByDictionaryAndWordStartingWithAndLevel(dictionary, letters, vocabLevel, pageable);
    }
    
    /**
     * Count vocabulary by dictionary, word starting with letters, and level
     */
    public long countByDictionaryAndWordStartingWithAndLevel(Integer dictionaryId, List<String> letters, String level) {
        Dictionary dictionary = dictionaryRepository.findById(dictionaryId)
                .orElseThrow(() -> new RuntimeException("Dictionary not found: " + dictionaryId));
        Vocab.Level vocabLevel = Vocab.Level.valueOf(level.toUpperCase());
        
        return vocabRepository.countByDictionaryAndWordStartingWithAndLevel(dictionary, letters, vocabLevel);
    }
    
    /**
     * Find vocabulary by dictionary with advanced filtering
     */
    public Page<Vocab> findByDictionaryWithFilters(Integer dictionaryId, String search, String level, 
                                                   List<Integer> topicIds, Pageable pageable) {
        Dictionary dictionary = dictionaryRepository.findById(dictionaryId)
                .orElseThrow(() -> new RuntimeException("Dictionary not found: " + dictionaryId));
        
        // For now, implement basic filtering
        // TODO: Add more sophisticated filtering logic
        if (search != null && !search.isEmpty()) {
            return vocabRepository.findByDictionaryAndWordContainingIgnoreCase(dictionary, search, pageable);
        }
        
        if (level != null && !level.isEmpty()) {
            Vocab.Level vocabLevel = Vocab.Level.valueOf(level.toUpperCase());
            return vocabRepository.findByDictionaryAndLevel(dictionary, vocabLevel, pageable);
        }
        
        if (topicIds != null && !topicIds.isEmpty()) {
            // TODO: Implement topic-based filtering
            return vocabRepository.findByDictionary(dictionary, pageable);
        }
        
        return vocabRepository.findByDictionary(dictionary, pageable);
    }
    
    /**
     * Get vocabulary count by topics for a dictionary
     */
    public Map<Integer, Long> getVocabCountByTopicsForDictionary(Integer dictionaryId) {
        Dictionary dictionary = dictionaryRepository.findById(dictionaryId)
                .orElseThrow(() -> new RuntimeException("Dictionary not found: " + dictionaryId));
        
        Map<Integer, Long> topicCounts = new HashMap<>();
        
        // Get all active topics
        List<Topics> topics = topicsRepository.findByStatus(Topics.Status.ACTIVE);
        
        for (Topics topic : topics) {
            // Count vocabulary for this topic in this dictionary
            long count = vocabTopicsRepository.countByTopicIdAndVocabDictionary(topic.getTopicId(), dictionary);
            topicCounts.put(topic.getTopicId(), count);
        }
        
        return topicCounts;
    }
    
    /**
     * Get vocabulary counts by topics and level for dictionary
     * Returns a map with format: topicId -> count for specific level
     */
    public Map<Integer, Long> getVocabCountByTopicsAndLevelForDictionary(Integer dictionaryId, String level) {
        Dictionary dictionary = dictionaryRepository.findById(dictionaryId)
                .orElseThrow(() -> new RuntimeException("Dictionary not found: " + dictionaryId));
        
        Vocab.Level vocabLevel = Vocab.Level.valueOf(level.toUpperCase());
        Map<Integer, Long> topicCounts = new HashMap<>();
        
        // Get all active topics
        List<Topics> topics = topicsRepository.findByStatus(Topics.Status.ACTIVE);
        
        for (Topics topic : topics) {
            // Count vocabulary for this topic in this dictionary with level filter
            long count = vocabRepository.countByDictionaryAndTopicsIdInAndLevel(dictionary, List.of(topic.getTopicId()), vocabLevel);
            topicCounts.put(topic.getTopicId(), count);
        }
        
        return topicCounts;
    }

    /**
     * Get vocabulary counts grouped by their first letter
     */
    public Map<String, Long> getVocabCountByFirstLetter(Integer dictionaryId) {
        Dictionary dictionary = dictionaryRepository.findById(dictionaryId)
                .orElseThrow(() -> new RuntimeException("Dictionary not found: " + dictionaryId));

        Map<String, Long> letterCounts = new HashMap<>();
        List<Object[]> results = vocabRepository.countByDictionaryGroupedByFirstLetter(dictionary);

        for (Object[] row : results) {
            String letter = (String) row[0];
            Long count = (Long) row[1];
            letterCounts.put(letter, count);
        }

        return letterCounts;
    }
}
