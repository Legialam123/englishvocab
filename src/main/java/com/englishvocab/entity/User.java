package com.englishvocab.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
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

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;
    
    @Column(unique = true, nullable = false)
    @NotBlank(message = "Username không được để trống")
    @Size(min = 3, max = 255, message = "Username phải có độ dài từ 3-255 ký tự") // Increased for email
    String username;
    
    @Column(nullable = true) // Nullable for Google users  
    String password;
    
    @Column(nullable = false)
    String fullname;
    
    @Column(unique = true, nullable = false)
    @Email(message = "Email không hợp lệ")
    String email;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    Role role = Role.USER;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    Status status = Status.ACTIVE;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @Column(name = "google_id", unique = true)
    String googleId;

    @Column(name = "google_user", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    boolean googleUser = false;
    
    public enum Role {
        USER, ADMIN
    }
    
    public enum Status {
        ACTIVE, LOCKED, DELETED
    }
    
    /**
     * Kiểm tra xem user có phải Google user không
     */
    public boolean isGoogleUser() {
        return googleUser;
    }
    
    /**
     * Kiểm tra xem user có thể đăng nhập bằng form không
     */
    public boolean canLoginWithForm() {
        return password != null && status == Status.ACTIVE;
    }
}