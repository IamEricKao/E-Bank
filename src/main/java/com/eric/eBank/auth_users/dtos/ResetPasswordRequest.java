package com.eric.eBank.auth_users.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResetPasswordRequest {
    // 用於找回密碼的請求
    private String email;

    // 安全碼
    private String code;
    private String newPassword;
}
