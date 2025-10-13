package com.englishvocab.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Entity
@Table(name = "topics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class Topics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "topic_id")
    Integer topicId;
    
    @Column(nullable = false, length = 100)
    @NotBlank(message = "Tên chủ đề không được để trống")
    @Size(max = 100, message = "Tên chủ đề không được vượt quá 100 ký tự")
    String name;
    
    @Column(length = 255)
    @Size(max = 255, message = "Mô tả không được vượt quá 255 ký tự")
    String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    Status status = Status.ACTIVE;
    
    // Relationships
    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<VocabTopics> vocabTopics;
    
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
