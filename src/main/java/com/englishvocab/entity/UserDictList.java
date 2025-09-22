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
@Table(name = "user_dict_list")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDictList {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dict_list_id")
    private Integer dictListId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dictionary_id", nullable = false)
    private Dictionary dictionary;
    
    @Column(nullable = false, length = 100)
    @NotBlank(message = "Tên danh sách không được để trống")
    @Size(max = 100, message = "Tên danh sách không được vượt quá 100 ký tự")
    private String name;
    
    @Column(length = 150)
    @Size(max = 150, message = "Mô tả không được vượt quá 150 ký tự")
    private String description;
    
    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Visibility visibility = Visibility.PRIVATE;
    
    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.ACTIVE;
    
    // Relationships
    @OneToMany(mappedBy = "userDictList", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ListVocab> listVocabs;
    
    public enum Visibility {
        PUBLIC, PRIVATE
    }
    
    public enum Status {
        ACTIVE, INACTIVE, ARCHIVED
    }
    
    /**
     * Convenience methods
     */
    public boolean isPublic() {
        return visibility == Visibility.PUBLIC;
    }
    
    public boolean isActive() {
        return status == Status.ACTIVE;
    }
    
    public int getVocabCount() {
        return listVocabs != null ? listVocabs.size() : 0;
    }
    
    public String getDisplayName() {
        return name + " (" + getVocabCount() + " từ)";
    }
}
