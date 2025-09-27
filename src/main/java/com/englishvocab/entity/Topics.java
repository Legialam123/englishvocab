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
    
    @Column(length = 255)
    @Size(max = 255, message = "Mô tả không được vượt quá 255 ký tự")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.ACTIVE;
    
    // Relationships
    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VocabTopics> vocabTopics;
    
    /**
     * Status enum for topics
     */
    public enum Status {
        ACTIVE, INACTIVE
    }
    
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
