package com.englishvocab.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    
    @NotBlank(message = "Username không được để trống")
    @Size(min = 3, max = 20, message = "Username phải có độ dài từ 3-20 ký tự")
    private String username;
    
    @NotBlank(message = "Password không được để trống")
    @Size(min = 6, message = "Password phải có ít nhất 6 ký tự")
    private String password;
    
    @NotBlank(message = "Xác nhận password không được để trống")
    private String confirmPassword;
    
    @NotBlank(message = "Họ tên không được để trống")
    private String fullname;
    
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;
}
