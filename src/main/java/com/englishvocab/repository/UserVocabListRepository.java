package com.englishvocab.repository;

import com.englishvocab.entity.Dictionary;
import com.englishvocab.entity.User;
import com.englishvocab.entity.UserVocabList;
import com.englishvocab.entity.UserVocabList.Status;
import com.englishvocab.entity.UserVocabList.Visibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing UserVocabList entities.
 * Supports multiple custom vocabulary lists per user (Option 2).
 */
@Repository
public interface UserVocabListRepository extends JpaRepository<UserVocabList, Integer> {

    /**
     * Find all lists belonging to a user (all statuses)
     */
    List<UserVocabList> findByUserOrderByCreatedAtDesc(User user);
    
    /**
     * Find all lists belonging to a user ordered by last updated
     */
    List<UserVocabList> findByUserOrderByUpdatedAtDesc(User user);

    /**
     * Find active lists only
     */
    List<UserVocabList> findByUserAndStatusOrderByCreatedAtDesc(User user, Status status);

    /**
     * Find by user and ID (for security - ensures user owns the list)
     */
    Optional<UserVocabList> findByUserVocabListIdAndUser(Integer listId, User user);

    /**
     * Check if a list name already exists for a user
     * (list names must be unique per user)
     */
    boolean existsByUserAndName(User user, String name);

    /**
     * Find public lists (for sharing/browsing)
     */
    List<UserVocabList> findByVisibilityAndStatusOrderByCreatedAtDesc(Visibility visibility, Status status);

    /**
     * Find public lists by user
     */
    List<UserVocabList> findByUserAndVisibilityOrderByCreatedAtDesc(User user, Visibility visibility);

    /**
     * Count total lists for a user
     */
    long countByUser(User user);

    /**
     * Count active lists for a user
     */
    long countByUserAndStatus(User user, Status status);

    /**
     * Get total vocabulary entries across all user's lists
     * Note: This counts all vocab entries, including duplicates across different lists
     * Example: If "hello" is in 2 lists, it counts as 2
     * Using COALESCE to handle null/empty collections
     */
    @Query("SELECT COALESCE(SUM(SIZE(uvl.dictVocabLists) + SIZE(uvl.customVocabLists)), 0) " +
           "FROM UserVocabList uvl WHERE uvl.user = :user")
    Long getTotalVocabEntriesAcrossAllLists(@Param("user") User user);

    /**
     * Search lists by name (case-insensitive)
     */
    List<UserVocabList> findByUserAndNameContainingIgnoreCase(User user, String keyword);
}
