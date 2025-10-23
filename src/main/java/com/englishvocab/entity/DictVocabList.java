package com.englishvocab.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Junction table: Liên kết system Vocab với UserVocabList
 * 1 vocab có thể thuộc nhiều user lists khác nhau
 */
@Entity
@Table(name = "dict_vocab_list",
       uniqueConstraints = @UniqueConstraint(columnNames = {"vocab_id", "user_vocab_list_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class DictVocabList {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dict_list_id")
    Integer dictListId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocab_id", nullable = false)
    Vocab vocab;
    
    @ManyToOne(fetch = FetchType.LAZY)  
    @JoinColumn(name = "user_vocab_list_id", nullable = false)
    UserVocabList userVocabList;
    
    @CreationTimestamp
    @Column(name = "added_at", updatable = false)
    LocalDateTime addedAt;
    
    /**
     * Helper methods
     */
    public String getVocabWord() {
        return vocab != null ? vocab.getWord() : "";
    }
    
    public String getListName() {
        return userVocabList != null ? userVocabList.getName() : "";
    }
}
