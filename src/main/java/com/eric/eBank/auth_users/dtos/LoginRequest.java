package com.eric.eBank.auth_users.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "電子郵件必填")
    @Email
    private String email;

    @NotBlank(message = "密碼必填")
    private String password;
}
