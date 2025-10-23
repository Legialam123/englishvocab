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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "dictionaries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Dictionary {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dictionary_id")
    Integer dictionaryId;
    
    @Column(nullable = false)
    @NotBlank(message = "Tên từ điển không được để trống")
    @Size(max = 100, message = "Tên từ điển không được vượt quá 100 ký tự")
    String name;
    
    @Column(name = "code", length = 50)
    @Size(max = 50, message = "Mã từ điển không được vượt quá 50 ký tự")
    String code;
    
    @Column(length = 100)
    @Size(max = 100, message = "Nhà xuất bản không được vượt quá 100 ký tự")
    String publisher;
    
    @Column(length = 10)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    Status status = Status.ACTIVE;
    
    @Column(name = "description", length = 150)
    @Size(max = 150, message = "Mô tả không được vượt quá 150 ký tự")
    String description;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
    
    // Relationships
    @OneToMany(mappedBy = "dictionary", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    List<Vocab> vocabularies;
    
    public enum Status {
        ACTIVE, INACTIVE, ARCHIVED
    }
    
    /**
     * Convenience methods
     */
    public boolean isActive() {
        return status == Status.ACTIVE;
    }
    
    public String getDisplayName() {
        return code != null ? name + " (" + code + ")" : name;
    }
}
