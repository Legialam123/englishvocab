package com.englishvocab.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dict_vocab_list")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DictVocabList {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dict_list_id") // This should be composite key, but simplified
    private Integer dictListId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocab_id", nullable = false)
    private Vocab vocab;
    
    @ManyToOne(fetch = FetchType.LAZY)  
    @JoinColumn(name = "dict_list_id", insertable = false, updatable = false)
    private UserVocabList userVocabList;
}
