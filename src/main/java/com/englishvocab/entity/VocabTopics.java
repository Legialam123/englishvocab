package com.englishvocab.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "vocab_topics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(VocabTopics.VocabTopicId.class)
public class VocabTopics {
    
    @Id
    @Column(name = "vocab_id")
    private Integer vocabId;
    
    @Id
    @Column(name = "topic_id")
    private Integer topicId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocab_id", insertable = false, updatable = false)
    private Vocab vocab;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", insertable = false, updatable = false)
    private Topics topic;
    
    /**
     * Composite Key Class
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VocabTopicId implements Serializable {
        private Integer vocabId;
        private Integer topicId;
    }
}
