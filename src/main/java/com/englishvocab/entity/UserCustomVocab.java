package com.englishvocab.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_custom_vocab")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCustomVocab {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "custom_vocab_id")
    private Integer customVocabId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, length = 100)
    @NotBlank(message = "Từ vựng không được để trống")
    @Size(max = 100, message = "Từ vựng không được vượt quá 100 ký tự")
    private String name; // The custom word
    
    @Column(nullable = false, length = 100)
    @NotBlank(message = "IPA không được để trống")
    @Size(max = 100, message = "IPA không được vượt quá 100 ký tự")
    private String ipa;
    
    @Column(length = 20)
    @Size(max = 20, message = "Từ loại không được vượt quá 20 ký tự")
    private String pos; // Part of Speech
    
    @Column(name = "meaning_vi", length = 50)
    @Size(max = 50, message = "Nghĩa tiếng Việt không được vượt quá 50 ký tự")
    private String meaningVi;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Convenience methods
     */
    public String getDisplayWord() {
        return name != null ? name : "";
    }
    
    public String getFormattedIpa() {
        return ipa != null ? "/" + ipa + "/" : "";
    }
    
    public String getDisplayInfo() {
        StringBuilder info = new StringBuilder();
        info.append(name);
        
        if (pos != null && !pos.isEmpty()) {
            info.append(" (").append(pos).append(")");
        }
        
        if (ipa != null && !ipa.isEmpty()) {
            info.append(" /").append(ipa).append("/");
        }
        
        if (meaningVi != null && !meaningVi.isEmpty()) {
            info.append(" - ").append(meaningVi);
        }
        
        return info.toString();
    }
}
