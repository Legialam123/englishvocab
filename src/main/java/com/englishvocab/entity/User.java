package com.englishvocab.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    @NotBlank(message = "Username không được để trống")
    @Size(min = 3, max = 255, message = "Username phải có độ dài từ 3-255 ký tự") // Increased for email
    private String username;
    
    @Column(nullable = true) // Nullable for Google users  
    private String password;
    
    @Column(nullable = false)
    private String fullname;
    
    @Column(unique = true, nullable = false)
    @Email(message = "Email không hợp lệ")
    private String email;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.ACTIVE;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "google_id", unique = true)
    private String googleId;

    @Column(name = "google_user", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean googleUser = false;
    
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