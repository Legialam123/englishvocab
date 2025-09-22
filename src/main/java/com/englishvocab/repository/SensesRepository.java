package com.englishvocab.repository;

import com.englishvocab.entity.Senses;
import com.englishvocab.entity.Vocab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SensesRepository extends JpaRepository<Senses, Integer> {
    
    /**
     * Tìm senses theo vocab
     */
    List<Senses> findByVocab(Vocab vocab);
    
    /**
     * Tìm senses theo vocab sắp xếp theo id
     */
    List<Senses> findByVocabOrderBySenseId(Vocab vocab);
    
    /**
     * Tìm theo nghĩa tiếng Việt
     */
    @Query("SELECT s FROM Senses s WHERE LOWER(s.meaningVi) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Senses> findByMeaningViContainingIgnoreCase(@Param("keyword") String keyword);
    
    /**
     * Tìm theo definition tiếng Anh
     */
    @Query("SELECT s FROM Senses s WHERE LOWER(s.definition) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Senses> findByDefinitionContainingIgnoreCase(@Param("keyword") String keyword);
    
    /**
     * Tìm senses có đầy đủ nghĩa VI và definition EN
     */
    @Query("SELECT s FROM Senses s WHERE s.meaningVi IS NOT NULL AND s.definition IS NOT NULL")
    List<Senses> findCompleteSenses();
    
    /**
     * Tìm senses chỉ có nghĩa tiếng Việt
     */
    @Query("SELECT s FROM Senses s WHERE s.meaningVi IS NOT NULL AND s.definition IS NULL")
    List<Senses> findVietnameseMeaningOnly();
    
    /**
     * Tìm senses chỉ có definition tiếng Anh
     */
    @Query("SELECT s FROM Senses s WHERE s.definition IS NOT NULL AND s.meaningVi IS NULL")
    List<Senses> findEnglishDefinitionOnly();
    
    /**
     * Đếm senses theo vocab
     */
    long countByVocab(Vocab vocab);
}
