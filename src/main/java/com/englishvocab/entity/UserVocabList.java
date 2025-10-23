package com.englishvocab.entity;

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

/**
 * Entity đại diện cho danh sách từ vựng cá nhân của user.
 * User có thể tạo nhiều lists với tên riêng.
 * Mỗi list có thể chứa:
 * - System vocab: Từ vựng từ dictionaries (qua DictVocabList)
 * - Custom vocab: Từ vựng tự thêm (qua CustomVocabList)
 * 
 * Không ràng buộc với dictionary cụ thể - user tự do mix vocab từ nhiều nguồn.
 */
@Entity
@Table(name = "user_dict_list")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class UserVocabList {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_vocab_list_id")
    Integer userVocabListId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;
    
    @Column(nullable = false, length = 100)
    @NotBlank(message = "Tên danh sách không được để trống")
    @Size(max = 100, message = "Tên danh sách không được vượt quá 100 ký tự")
    String name;
    
    @Column(length = 150)
    @Size(max = 150, message = "Mô tả không được vượt quá 150 ký tự")
    String description;
    
    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    Visibility visibility = Visibility.PRIVATE;
    
    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    Status status = Status.ACTIVE;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
    
    // Relationships
    @OneToMany(mappedBy = "userVocabList", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    List<DictVocabList> dictVocabLists;

    @OneToMany(mappedBy = "userVocabList", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    List<CustomVocabList> customVocabLists;
    
    public enum Visibility {
        PUBLIC,  // Ai cũng có thể xem
        PRIVATE  // Chỉ owner xem được
    }
    
    public enum Status {
        ACTIVE,    // Đang sử dụng
        INACTIVE,  // Tạm ngưng
        ARCHIVED   // Lưu trữ (không hiện trong danh sách chính)
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
    
    public int getTotalVocabCount() {
        return getDictVocabListCount() + getCustomVocabListCount();
    }
    
    public int getDictVocabListCount() {
        return dictVocabLists != null ? dictVocabLists.size() : 0;
    }

    public int getCustomVocabListCount() {
        return customVocabLists != null ? customVocabLists.size() : 0;
    }
}
