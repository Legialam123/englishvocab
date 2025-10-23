package com.englishvocab.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "vocab")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "dictionary", "senses", "vocabTopics", "userProgress", "listVocabs"})
public class Vocab {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vocab_id")
    Integer vocabId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "dictionary_id", nullable = false)
    Dictionary dictionary;
    
    @Column(nullable = false, length = 100)
    @NotBlank(message = "Từ vựng không được để trống")
    @Size(max = 100, message = "Từ vựng không được vượt quá 100 ký tự")
    String word; // The actual word/term
    
    @Column(nullable = false, length = 20)
    @NotBlank(message = "Từ loại không được để trống")
    @Size(max = 20, message = "Từ loại không được vượt quá 20 ký tự")
    String pos; // Part of Speech (noun, verb, adj, etc.)
    
    @Column(length = 100)
    @Size(max = 100, message = "Phiên âm không được vượt quá 100 ký tự")
    String ipa; // International Phonetic Alphabet
    
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    Level level = Level.BEGINNER;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
    
    // Relationships
    @OneToMany(mappedBy = "vocab", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Senses> senses;
    
    @OneToMany(mappedBy = "vocab", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    List<VocabTopics> vocabTopics;
    
    @OneToMany(mappedBy = "vocab", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    List<UserVocabProgress> userProgress;
    
    @OneToMany(mappedBy = "vocab", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    List<DictVocabList> listVocabs;
    
    public enum Level {
        BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
    }
    
    /**
     * Convenience methods
     */
    public String getPrimaryMeaning() {
        if (senses != null && !senses.isEmpty()) {
            return senses.get(0).getMeaningVi();
        }
        return "";
    }
    
    public String getPrimarySense() {
        return getPrimaryMeaning();
    }
    
    public String getDefinition() {
        if (senses != null && !senses.isEmpty()) {
            return senses.get(0).getDefinition();
        }
        return null;
    }
    
    public String getExample() {
        // Example is not stored in Senses entity currently
        // Return null for now, can be added later if needed
        return null;
    }
    
    public String getDisplayWord() {
        return word != null ? word : "";
    }
    
    public String getFormattedIpa() {
        return ipa != null ? "/" + ipa + "/" : "";
    }
}
