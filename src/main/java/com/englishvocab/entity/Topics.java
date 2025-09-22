package com.englishvocab.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "topics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Topics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "topic_id")
    private Integer topicId;
    
    @Column(nullable = false, length = 100)
    @NotBlank(message = "Tên chủ đề không được để trống")
    @Size(max = 100, message = "Tên chủ đề không được vượt quá 100 ký tự")
    private String name;
    
    // Relationships
    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VocabTopics> vocabTopics;
    
    /**
     * Convenience methods
     */
    public String getDisplayName() {
        return name != null ? name : "Unknown Topic";
    }
    
    public int getVocabCount() {
        return vocabTopics != null ? vocabTopics.size() : 0;
    }
}
