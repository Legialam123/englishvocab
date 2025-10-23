package com.englishvocab.repository;

import com.englishvocab.entity.DictVocabList;
import com.englishvocab.entity.UserVocabList;
import com.englishvocab.entity.Vocab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing DictVocabList junction table.
 * Links system Vocab to UserVocabList.
 */
@Repository
public interface DictVocabListRepository extends JpaRepository<DictVocabList, Integer> {

    /**
     * Find all system vocabularies in a list
     */
    List<DictVocabList> findByUserVocabListOrderByAddedAtDesc(UserVocabList list);

    /**
     * Find specific vocab-list link
     */
    Optional<DictVocabList> findByUserVocabListAndVocab(UserVocabList list, Vocab vocab);

    /**
     * Check if a vocab is already in a list
     */
    boolean existsByUserVocabListAndVocab(UserVocabList list, Vocab vocab);

    /**
     * Remove a vocab from a list
     */
    void deleteByUserVocabListAndVocab(UserVocabList list, Vocab vocab);

    /**
     * Remove all vocabs from a list
     */
    void deleteByUserVocabList(UserVocabList list);

    /**
     * Count system vocabularies in a list
     */
    long countByUserVocabList(UserVocabList list);

    /**
     * Find all lists containing a specific vocab
     */
    List<DictVocabList> findByVocab(Vocab vocab);

    /**
     * Find all lists containing a vocab for a specific user
     */
    @Query("SELECT dvl FROM DictVocabList dvl " +
           "WHERE dvl.vocab = :vocab AND dvl.userVocabList.user.id = :userId")
    List<DictVocabList> findByVocabAndUserId(@Param("vocab") Vocab vocab, @Param("userId") String userId);

    /**
     * Check if any list contains this vocab
     */
    boolean existsByVocab(Vocab vocab);
}
