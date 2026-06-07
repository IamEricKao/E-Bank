package com.eric.eBank.auth_users.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdatePasswordRequest {
    @NotBlank(message = "舊密碼必填")
    private String oldPassword;

    @NotBlank(message = "新密碼必填")
    private String newPassword;
}
