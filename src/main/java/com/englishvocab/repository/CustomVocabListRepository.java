package com.englishvocab.repository;

import com.englishvocab.entity.CustomVocabList;
import com.englishvocab.entity.UserCustomVocab;
import com.englishvocab.entity.UserVocabList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing CustomVocabList junction table.
 * Links UserCustomVocab to UserVocabList.
 */
@Repository
public interface CustomVocabListRepository extends JpaRepository<CustomVocabList, Integer> {

    /**
     * Find all custom vocabularies in a list
     */
    List<CustomVocabList> findByUserVocabListOrderByAddedAtDesc(UserVocabList list);

    /**
     * Find specific custom-vocab-list link
     */
    Optional<CustomVocabList> findByUserVocabListAndCustomVocab(UserVocabList list, UserCustomVocab customVocab);

    /**
     * Check if a custom vocab is already in a list
     */
    boolean existsByUserVocabListAndCustomVocab(UserVocabList list, UserCustomVocab customVocab);

    /**
     * Remove a custom vocab from a list
     */
    void deleteByUserVocabListAndCustomVocab(UserVocabList list, UserCustomVocab customVocab);

    /**
     * Remove all custom vocabs from a list
     */
    void deleteByUserVocabList(UserVocabList list);

    /**
     * Count custom vocabularies in a list
     */
    long countByUserVocabList(UserVocabList list);

    /**
     * Find all lists containing a specific custom vocab
     */
    List<CustomVocabList> findByCustomVocab(UserCustomVocab customVocab);

    /**
     * Find all lists containing a custom vocab for a specific user
     */
    @Query("SELECT cvl FROM CustomVocabList cvl " +
           "WHERE cvl.customVocab = :customVocab AND cvl.userVocabList.user.id = :userId")
    List<CustomVocabList> findByCustomVocabAndUserId(@Param("customVocab") UserCustomVocab customVocab, 
                                                       @Param("userId") String userId);

    /**
     * Check if any list contains this custom vocab
     */
    boolean existsByCustomVocab(UserCustomVocab customVocab);
}
