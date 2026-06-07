package com.eric.eBank.auth_users.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class RegistrationRequest {

    @NotBlank(message = "姓氏不能為空")
    private String firstName;

    private String lastName;

    private String phoneNumber;

    @NotBlank(message = "電子郵件不能為空")
    @Email
    private String email;

    private List<String> roles;

    @NotBlank(message = "密碼不能為空")
    private String password;
}
