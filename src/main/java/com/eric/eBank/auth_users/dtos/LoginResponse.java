package com.eric.eBank.auth_users.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LoginResponse {
    private String token;
    private List<String> roles;
}
