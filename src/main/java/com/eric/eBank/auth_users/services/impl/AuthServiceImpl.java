package com.eric.eBank.auth_users.services.impl;

import com.eric.eBank.account.entity.Account;
import com.eric.eBank.account.services.AccountService;
import com.eric.eBank.auth_users.dtos.LoginRequest;
import com.eric.eBank.auth_users.dtos.LoginResponse;
import com.eric.eBank.auth_users.dtos.RegistrationRequest;
import com.eric.eBank.auth_users.dtos.ResetPasswordRequest;
import com.eric.eBank.auth_users.entity.PasswordResetCode;
import com.eric.eBank.auth_users.entity.User;
import com.eric.eBank.auth_users.repo.PasswordResetCodeRepo;
import com.eric.eBank.auth_users.repo.UserRepo;
import com.eric.eBank.auth_users.services.AuthService;
import com.eric.eBank.auth_users.services.CodeGenerator;
import com.eric.eBank.enums.AccountType;
import com.eric.eBank.enums.Currency;
import com.eric.eBank.exceptions.BadRequestException;
import com.eric.eBank.exceptions.NotFoundException;
import com.eric.eBank.notification.dtos.NotificationDTO;
import com.eric.eBank.notification.services.NotificationService;
import com.eric.eBank.res.Response;
import com.eric.eBank.role.entity.Role;
import com.eric.eBank.role.repo.RoleRepo;
import com.eric.eBank.security.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepo userRepo;
    private final RoleRepo roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final TokenService tokenService;
    private final AccountService accountService;
    private final PasswordResetCodeRepo passwordResetCodeRepo;
    private final CodeGenerator codeGenerator;

    @Value("${password.reset.link}")
    private String resetLink;

    @Override
    public Response<String> register(RegistrationRequest request) {
        List<Role> roles;
        if (request.getRoles() == null || request.getRoles().isEmpty()) {
            Role defaultRole = roleRepo.findByName("CUSTOMER")
                    .orElseThrow(() -> new BadRequestException("顧客權限不存在"));
            roles = Collections.singletonList(defaultRole);
        } else {
            roles = request.getRoles().stream()
                    .map(roleName -> roleRepo.findByName(roleName)
                            .orElseThrow(() -> new BadRequestException(" 權限不存在: " + roleName)))
                    .toList();
        }

        if (userRepo.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("電子郵件已被使用");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(roles)
                .active(true)
                .build();

        User savedUser = userRepo.save(user);

        // 新增帳戶
        Account savedAccount = accountService.createAccount(AccountType.SAVINGS, user);

        // region 寄送Email
        // 歡迎信件
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", savedUser.getFirstName());

        NotificationDTO notificationDTO = NotificationDTO.builder()
                .recipient(savedUser.getEmail())
                .subject("歡迎使用eBank🎉")
                .templateName("welcome")
                .templateVariables(vars)
                .build();

        notificationService.sendEmail(notificationDTO, savedUser);

        // 註冊成功通知
        Map<String, Object> accountVars = new HashMap<>();
        accountVars.put("name", savedUser.getFirstName());
        accountVars.put("accountNumber", savedAccount.getAccountNumber());
        accountVars.put("accountType", AccountType.SAVINGS.getChinese());
        accountVars.put("currency", Currency.TWD.getChinese());

        NotificationDTO accountNotificationDTO = NotificationDTO.builder()
                .recipient(savedUser.getEmail())
                .subject("註冊成功✅")
                .templateName("account-created")
                .templateVariables(accountVars)
                .build();

        notificationService.sendEmail(accountNotificationDTO, savedUser);

        // endregion 寄送Email

        return Response.<String>builder()
                .statusCode(HttpStatus.OK.value())
                .message("註冊成功")
                .data("你的帳戶號碼: " + savedAccount.getAccountNumber() + " ，更多資訊請查看您的電子郵件。")
                .build();

    }

    @Override
    public Response<LoginResponse> login(LoginRequest loginRequest) {
        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("帳號錯誤"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new NotFoundException("密碼錯誤");
        }

        String token = tokenService.generateToken(user.getEmail());

        LoginResponse loginResponse = LoginResponse.builder()
                .token(token)
                .roles(user.getRoles().stream().map(Role::getName).toList())
                .build();

        return Response.<LoginResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("登入成功")
                .data(loginResponse)
                .build();
    }

    @Override
    @Transactional
    public Response<?> forgetPassword(String email) {
        User user = userRepo.findByEmail(email).orElseThrow(() -> new NotFoundException("找不到使用者"));
        passwordResetCodeRepo.deleteByUserId(user.getId());

        String code = codeGenerator.generateUniqueCode();

        PasswordResetCode resetCode = PasswordResetCode.builder()
                .code(code)
                .user(user)
                .expiryDate(calculateExpiryDate())
                .build();

        passwordResetCodeRepo.save(resetCode);

        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("name", user.getFirstName());
        templateVariables.put("resetLink", resetLink + code);

        NotificationDTO notificationDTO = NotificationDTO.builder()
                .recipient(user.getEmail())
                .subject("密碼重置")
                .templateName("password-reset")
                .templateVariables(templateVariables)
                .build();

        notificationService.sendEmail(notificationDTO, user);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("重置碼已寄送至您的電子信箱")
                .build();
    }

    @Override
    @Transactional
    public Response<?> updatePasswordViaResetCode(ResetPasswordRequest resetPasswordRequest) {
        String code = resetPasswordRequest.getCode();
        String newPassword = resetPasswordRequest.getNewPassword();

        PasswordResetCode resetCode = passwordResetCodeRepo.findByCode(code)
                .orElseThrow(() -> new NotFoundException("無效的重置碼"));

        if (resetCode.getExpiryDate().isBefore(LocalDateTime.now())) {
            passwordResetCodeRepo.delete(resetCode);
            throw new BadRequestException("重置碼已過期");
        }

        User user = resetCode.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        passwordResetCodeRepo.delete(resetCode);

        Map<String, Object> pwdConfirmVariables = new HashMap<>();
        pwdConfirmVariables.put("name", user.getFirstName());

        NotificationDTO pwdConfirmDTO = NotificationDTO.builder()
                .recipient(user.getEmail())
                .subject("密碼更新成功")
                .templateName("password-update-confirmation")
                .templateVariables(pwdConfirmVariables)
                .build();

        notificationService.sendEmail(pwdConfirmDTO, user);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("密碼更新成功")
                .build();
    }

    /**
     * 計算驗證碼的過期時間，
     * 預設五小時後過期
     *
     * @return LocalDateTime
     */
    private LocalDateTime calculateExpiryDate() {
        return LocalDateTime.now().plusHours(5);
    }

}
