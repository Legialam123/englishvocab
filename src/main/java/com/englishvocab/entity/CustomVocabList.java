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
 * Junction table: Liên kết UserCustomVocab với UserVocabList
 * 1 custom vocab có thể thuộc nhiều lists khác nhau
 */
@Entity
@Table(name = "custom_vocab_list", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"custom_vocab_id", "user_vocab_list_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class CustomVocabList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "custom_list_id")
    Integer customListId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_vocab_id", nullable = false)
    UserCustomVocab customVocab;
    
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
        return customVocab != null ? customVocab.getName() : "";
    }
    
    public String getListName() {
        return userVocabList != null ? userVocabList.getName() : "";
    }
}
