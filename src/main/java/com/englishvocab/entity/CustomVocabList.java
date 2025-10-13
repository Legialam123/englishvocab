package com.englishvocab.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "custom_vocab_list")
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
}
