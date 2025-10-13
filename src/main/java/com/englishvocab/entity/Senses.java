package com.englishvocab.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "senses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class Senses {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sense_id")
    Integer senseId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocab_id", nullable = false)
    @JsonIgnore
    Vocab vocab;
    
    @Column(name = "meaning_vi", nullable = false, length = 50)
    @NotBlank(message = "Nghĩa tiếng Việt không được để trống")
    @Size(max = 50, message = "Nghĩa tiếng Việt không được vượt quá 50 ký tự")
    String meaningVi; // Vietnamese meaning
    
    @Column(name = "definition", length = 100)
    @Size(max = 100, message = "Định nghĩa không được vượt quá 100 ký tự")
    String definition; // English definition
    
    /**
     * Convenience methods
     */
    public String getDisplayMeaning() {
        if (meaningVi != null && definition != null) {
            return meaningVi + " (" + definition + ")";
        } else if (meaningVi != null) {
            return meaningVi;
        } else if (definition != null) {
            return definition;
        }
        return "";
    }
    
    public boolean hasVietnameseMeaning() {
        return meaningVi != null && !meaningVi.trim().isEmpty();
    }
    
    public boolean hasEnglishDefinition() {
        return definition != null && !definition.trim().isEmpty();
    }
}
