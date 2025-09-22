package com.englishvocab.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "vocab_topics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VocabTopics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vocab_id") // Note: This should be a composite key, but simplified for now
    private Integer vocabId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocab_id", insertable = false, updatable = false)
    private Vocab vocab;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topics topic;
    
    @Column(name = "topic_id", insertable = false, updatable = false)
    private Integer topicId;
}
