package com.eric.eBank.auth_users.services;

import com.eric.eBank.auth_users.repo.PasswordResetCodeRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 產生忘記密碼信件中的驗證碼，
 * 驗證密碼長度為5位數，
 * 包含大寫英文字母跟數字，
 * 並且不重複驗證碼
 */

@Component
@RequiredArgsConstructor
public class CodeGenerator {

    private PasswordResetCodeRepo passwordResetCodeRepo;

    private static final String ALPHA_NUMBER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 5;

    public String generateUniqueCode() {
        String code;
        do {
            code = generateRandomCode();
        } while (passwordResetCodeRepo.findByCode(code).isPresent());

        return code;
    }

    private String generateRandomCode() {
        StringBuilder sb = new StringBuilder();
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = random.nextInt(ALPHA_NUMBER.length());
            sb.append(ALPHA_NUMBER.charAt(index));
        }

        return sb.toString();
    }
}
