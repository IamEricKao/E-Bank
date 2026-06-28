package com.eric.eBank.auth_users.services;

import com.eric.eBank.auth_users.dtos.*;
import com.eric.eBank.res.Response;

public interface AuthService {
    Response<String> register(RegistrationRequest request);
    Response<LoginResponse> login(LoginRequest loginRequest);
    Response<?> forgetPassword(String email);
    Response<?> updatePasswordViaResetCode(ResetPasswordRequest resetPasswordRequest);
}
