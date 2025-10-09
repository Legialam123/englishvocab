package com.englishvocab.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "senses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Senses {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sense_id")
    private Integer senseId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocab_id", nullable = false)
    @JsonIgnore
    private Vocab vocab;
    
    @Column(name = "meaning_vi", nullable = false, length = 50)
    @NotBlank(message = "Nghĩa tiếng Việt không được để trống")
    @Size(max = 50, message = "Nghĩa tiếng Việt không được vượt quá 50 ký tự")
    private String meaningVi; // Vietnamese meaning
    
    @Column(name = "definition", length = 100)
    @Size(max = 100, message = "Định nghĩa không được vượt quá 100 ký tự")
    private String definition; // English definition
    
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
