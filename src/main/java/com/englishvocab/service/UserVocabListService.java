package com.englishvocab.service;

import com.englishvocab.dto.*;
import com.englishvocab.entity.*;
import com.englishvocab.entity.UserVocabList.Status;
import com.englishvocab.entity.UserVocabList.Visibility;
import com.englishvocab.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing user vocabulary lists (Option 2: Multiple Lists)
 * 
 * Features:
 * - Users can create multiple named lists
 * - Each list can contain system vocab, custom vocab, or both
 * - Lists can be dictionary-based (only vocabs from specific dictionary)
 * - Or custom-only (only user's custom vocabs)
 * - Lists can be private or public (shared)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserVocabListService {

    private final UserVocabListRepository listRepository;
    private final DictVocabListRepository dictVocabListRepository;
    private final CustomVocabListRepository customVocabListRepository;
    private final VocabularyService vocabularyService;
    private final UserCustomVocabService userCustomVocabService;
    private final UserVocabProgressRepository progressRepository;

    /**
     * Create a new vocabulary list
     */
    public UserVocabList createList(User user, CreateListRequest request) {
        if (listRepository.existsByUserAndName(user, request.getName())) {
            throw new IllegalArgumentException("List name already exists: " + request.getName());
        }
        
        UserVocabList list = UserVocabList.builder()
                .user(user)
                .name(request.getName())
                .description(request.getDescription())
                .visibility(request.getVisibility())
                .status(Status.ACTIVE)
                .build();
        
        return listRepository.save(list);
    }

    /**
     * Get all lists for a user (all statuses)
     */
    @Transactional(readOnly = true)
    public List<UserVocabList> getUserLists(User user) {
        return listRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Get active lists only
     */
    @Transactional(readOnly = true)
    public List<UserVocabList> getActiveLists(User user) {
        return listRepository.findByUserAndStatusOrderByCreatedAtDesc(user, Status.ACTIVE);
    }

    /**
     * Get list summaries with vocabulary counts
     */
    @Transactional(readOnly = true)
    public List<VocabListSummaryDTO> getListSummaries(User user) {
        List<UserVocabList> lists = getUserLists(user);
        
        return lists.stream()
                .map(list -> {
                    long systemCount = dictVocabListRepository.countByUserVocabList(list);
                    long customCount = customVocabListRepository.countByUserVocabList(list);
                    return VocabListSummaryDTO.from(list, systemCount, customCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get a single list by ID (with security check)
     */
    @Transactional(readOnly = true)
    public UserVocabList getListById(Integer listId, User user) {
        return listRepository.findByUserVocabListIdAndUser(listId, user)
                .orElseThrow(() -> new RuntimeException("List not found or access denied"));
    }

    /**
     * Add system vocabulary to a list
     */
    public void addSystemVocab(Integer listId, Integer vocabId, User user) {
        UserVocabList list = getListById(listId, user);
        Vocab vocab = vocabularyService.findByIdOrThrow(vocabId);
        
        // Check if already exists
        if (dictVocabListRepository.existsByUserVocabListAndVocab(list, vocab)) {
            return; // Silent skip
        }
        
        // Create link
        DictVocabList link = DictVocabList.builder()
                .userVocabList(list)
                .vocab(vocab)
                .build();
        
        dictVocabListRepository.save(link);
    }

    /**
     * Add system vocabulary to multiple lists
     * @return number of lists successfully added to
     */
    public int addSystemVocabToLists(String userId, Integer vocabId, List<Integer> listIds) {
        if (listIds == null || listIds.isEmpty()) {
            throw new IllegalArgumentException("List IDs cannot be empty");
        }
        
        User user = new User();
        user.setId(userId);
        
        int successCount = 0;
        for (Integer listId : listIds) {
            try {
                addSystemVocab(listId, vocabId, user);
                successCount++;
            } catch (Exception e) {
                log.warn("Failed to add vocab to list {}", listId);
            }
        }
        
        return successCount;
    }

    /**
     * Add custom vocabulary to a list
     */
    public void addCustomVocab(Integer listId, Integer customVocabId, User user) {
        UserVocabList list = getListById(listId, user);
        UserCustomVocab customVocab = userCustomVocabService.findByIdOrThrow(customVocabId);
        
        if (!customVocab.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Cannot add another user's custom vocabulary");
        }
        
        if (customVocabListRepository.existsByUserVocabListAndCustomVocab(list, customVocab)) {
            return; // Silent skip
        }
        
        CustomVocabList link = CustomVocabList.builder()
                .userVocabList(list)
                .customVocab(customVocab)
                .build();
        
        customVocabListRepository.save(link);
    }

    /**
     * Get all vocabulary in a list (unified view)
     */
    @Transactional(readOnly = true)
    public List<VocabularyItemDTO> getAllVocabulary(Integer listId, User user) {
        UserVocabList list = getListById(listId, user);
        
        List<VocabularyItemDTO> result = new ArrayList<>();
        
        // Add system vocabulary
        List<DictVocabList> systemVocabs = dictVocabListRepository.findByUserVocabListOrderByAddedAtDesc(list);
        result.addAll(systemVocabs.stream()
                .map(dv -> VocabularyItemDTO.fromSystemVocab(dv.getVocab(), dv.getAddedAt()))
                .collect(Collectors.toList()));
        
        // Add custom vocabulary
        List<CustomVocabList> customVocabs = customVocabListRepository.findByUserVocabListOrderByAddedAtDesc(list);
        result.addAll(customVocabs.stream()
                .map(cv -> VocabularyItemDTO.fromCustomVocab(cv.getCustomVocab(), cv.getAddedAt()))
                .collect(Collectors.toList()));
        
        return result;
    }

    /**
     * Remove system vocabulary from list
     */
    public void removeSystemVocab(Integer listId, Integer vocabId, User user) {
        UserVocabList list = getListById(listId, user);
        Vocab vocab = vocabularyService.findByIdOrThrow(vocabId);
        dictVocabListRepository.deleteByUserVocabListAndVocab(list, vocab);
    }

    /**
     * Remove custom vocabulary from list
     */
    public void removeCustomVocab(Integer listId, Integer customVocabId, User user) {
        UserVocabList list = getListById(listId, user);
        UserCustomVocab customVocab = userCustomVocabService.findByIdOrThrow(customVocabId);
        customVocabListRepository.deleteByUserVocabListAndCustomVocab(list, customVocab);
    }

    /**
     * Clear all vocabulary from a list
     */
    public void clearList(Integer listId, User user) {
        UserVocabList list = getListById(listId, user);
        dictVocabListRepository.deleteByUserVocabList(list);
        customVocabListRepository.deleteByUserVocabList(list);
    }

    /**
     * Update list metadata
     */
    public UserVocabList updateList(Integer listId, UpdateListRequest request, User user) {
        UserVocabList list = getListById(listId, user);
        
        if (!list.getName().equals(request.getName()) &&
            listRepository.existsByUserAndName(user, request.getName())) {
            throw new IllegalArgumentException("List name already exists: " + request.getName());
        }
        
        list.setName(request.getName());
        list.setDescription(request.getDescription());
        list.setVisibility(request.getVisibility());
        list.setStatus(request.getStatus());
        
        return listRepository.save(list);
    }

    /**
     * Archive a list (soft delete)
     */
    public void archiveList(Integer listId, User user) {
        UserVocabList list = getListById(listId, user);
        list.setStatus(Status.ARCHIVED);
        listRepository.save(list);
    }

    /**
     * Permanently delete a list
     */
    public void deleteList(Integer listId, User user) {
        UserVocabList list = getListById(listId, user);
        listRepository.delete(list);
    }

    /**
     * Get statistics for a list
     */
    @Transactional(readOnly = true)
    public ListStatistics getStatistics(Integer listId, User user) {
        UserVocabList list = getListById(listId, user);
        
        long systemCount = dictVocabListRepository.countByUserVocabList(list);
        long customCount = customVocabListRepository.countByUserVocabList(list);
        
        return ListStatistics.builder()
                .listId(listId)
                .listName(list.getName())
                .systemVocabCount(systemCount)
                .customVocabCount(customCount)
                .totalVocabCount(systemCount + customCount)
                .build();
    }

    /**
     * Get lists containing a specific system vocab
     */
    @Transactional(readOnly = true)
    public List<UserVocabList> getListsContainingVocab(Integer vocabId, User user) {
        Vocab vocab = vocabularyService.findByIdOrThrow(vocabId);
        
        return dictVocabListRepository.findByVocabAndUserId(vocab, user.getId())
                .stream()
                .map(DictVocabList::getUserVocabList)
                .collect(Collectors.toList());
    }

    /**
     * Get lists containing a specific custom vocab
     */
    @Transactional(readOnly = true)
    public List<UserVocabList> getListsContainingCustomVocab(Integer customVocabId, User user) {
        UserCustomVocab customVocab = userCustomVocabService.findByIdOrThrow(customVocabId);
        
        return customVocabListRepository.findByCustomVocabAndUserId(customVocab, user.getId())
                .stream()
                .map(CustomVocabList::getUserVocabList)
                .collect(Collectors.toList());
    }

    /**
     * Search lists by name
     */
    @Transactional(readOnly = true)
    public List<UserVocabList> searchLists(User user, String keyword) {
        return listRepository.findByUserAndNameContainingIgnoreCase(user, keyword);
    }

    /**
     * Get public lists (for browsing/sharing)
     */
    @Transactional(readOnly = true)
    public List<VocabListSummaryDTO> getPublicLists() {
        List<UserVocabList> lists = listRepository.findByVisibilityAndStatusOrderByCreatedAtDesc(
                Visibility.PUBLIC, Status.ACTIVE);
        
        return lists.stream()
                .map(list -> {
                    long systemCount = dictVocabListRepository.countByUserVocabList(list);
                    long customCount = customVocabListRepository.countByUserVocabList(list);
                    return VocabListSummaryDTO.from(list, systemCount, customCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get total vocabulary count across all user's lists
     */
    @Transactional(readOnly = true)
    public long getTotalVocabEntriesAcrossAllLists(User user) {
        Long count = listRepository.getTotalVocabEntriesAcrossAllLists(user);
        return count != null ? count : 0L;
    }

    /**
     * Đếm tổng số DANH SÁCH từ vựng của một user
     * (Count total number of vocabulary lists for a user)
     */
    @Transactional(readOnly = true)
    public long countUserLists(User user) {
        return listRepository.countByUser(user);
    }
    
    /**
     * Get dashboard statistics for a user
     */
    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats(User user) {
        // Total vocabulary the user has actually learned (from progress table)
        long totalVocab = progressRepository.countByUser(user);
        long totalLists = countUserLists(user);
        
        // Calculate study streak (consecutive days with learning activity)
        int studyStreak = calculateStudyStreak(user);
        
        // Calculate mastered count (vocabulary with MASTERED status)
        long masteredCount = calculateUniqueMasteredVocab(user);
        
        return DashboardStatsDTO.builder()
                .totalVocabCount(totalVocab)
                .totalListCount(totalLists)
                .studyStreak(studyStreak)
                .masteredVocabCount(masteredCount)
                .build();
    }
    
    /**
     * Calculate study streak - consecutive days with learning activity
     * Based on lastReviewed date in UserVocabProgress
     */
    @Transactional(readOnly = true)
    public int calculateStudyStreak(User user) {
        // Get recent activity (last 365 days)
        LocalDateTime oneYearAgo = LocalDateTime.now().minusDays(365);
        List<UserVocabProgress> recentProgress = progressRepository.findRecentActivity(user, oneYearAgo);
        
        if (recentProgress.isEmpty()) {
            return 0;
        }
        
        // Collect unique dates with activity
        java.util.Set<LocalDate> activeDates = new java.util.HashSet<>();
        for (UserVocabProgress progress : recentProgress) {
            if (progress.getLastReviewed() != null) {
                activeDates.add(progress.getLastReviewed().toLocalDate());
            }
        }
        
        // Calculate consecutive days from today backwards
        LocalDate today = LocalDate.now();
        int streak = 0;
        
        // Check if user studied today or yesterday (grace period)
        if (!activeDates.contains(today) && !activeDates.contains(today.minusDays(1))) {
            return 0; // Streak broken
        }
        
        // Count consecutive days
        LocalDate checkDate = today;
        while (activeDates.contains(checkDate)) {
            streak++;
            checkDate = checkDate.minusDays(1);
            
            // Limit check to prevent infinite loop
            if (streak > 365) break;
        }
        
        return streak;
    }
    
    /**
     * Calculate mastered vocabulary count
     * Based on UserVocabProgress with status = MASTERED
     */
    @Transactional(readOnly = true)
    public long calculateUniqueMasteredVocab(User user) {
        // Count vocabulary with MASTERED status in progress table
        return progressRepository.countByUserAndStatus(user, UserVocabProgress.Status.MASTERED);
    }
    
    /**
     * Get recent lists (top N most recently updated)
     */
    @Transactional(readOnly = true)
    public List<VocabListSummaryDTO> getRecentLists(User user, int limit) {
        List<UserVocabList> lists = listRepository.findByUserOrderByUpdatedAtDesc(user);
        
        return lists.stream()
                .limit(limit)
                .map(list -> {
                    long systemCount = dictVocabListRepository.countByUserVocabList(list);
                    long customCount = customVocabListRepository.countByUserVocabList(list);
                    return VocabListSummaryDTO.from(list, systemCount, customCount);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Get recently learned vocabulary from UserVocabProgress
     * This shows vocabulary that user has actually studied, not just added to lists
     */
    @Transactional(readOnly = true)
    public List<UserVocabProgress> getRecentlyLearnedVocabulary(User user, int limit) {
        // Get recent progress ordered by last reviewed date
        LocalDateTime oneYearAgo = LocalDateTime.now().minusDays(365);
        List<UserVocabProgress> recentProgress = progressRepository.findRecentActivity(user, oneYearAgo);
        
        // Return top N most recently reviewed
        return recentProgress.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
}
