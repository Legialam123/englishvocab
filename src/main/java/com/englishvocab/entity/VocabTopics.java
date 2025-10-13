package com.englishvocab.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;

@Entity
@Table(name = "vocab_topics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@IdClass(VocabTopics.VocabTopicId.class)
public class VocabTopics {
    
    @Id
    @Column(name = "vocab_id")
    Integer vocabId;
    
    @Id
    @Column(name = "topic_id")
    Integer topicId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocab_id", insertable = false, updatable = false)
    Vocab vocab;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", insertable = false, updatable = false)
    Topics topic;
    
    /**
     * Composite Key Class
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VocabTopicId implements Serializable {
        Integer vocabId;
        Integer topicId;
    }
}
