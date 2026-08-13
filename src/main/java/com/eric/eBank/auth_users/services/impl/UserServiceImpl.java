package com.eric.eBank.auth_users.services.impl;

import com.eric.eBank.auth_users.dtos.UpdatePasswordRequest;
import com.eric.eBank.auth_users.dtos.UserDTO;
import com.eric.eBank.auth_users.entity.User;
import com.eric.eBank.auth_users.repo.UserRepo;
import com.eric.eBank.auth_users.services.UserService;
import com.eric.eBank.exceptions.BadRequestException;
import com.eric.eBank.exceptions.NotFoundException;
import com.eric.eBank.notification.dtos.NotificationDTO;
import com.eric.eBank.notification.services.NotificationService;
import com.eric.eBank.res.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.http.HttpStatusCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepo userRepo;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    // 將上傳的檔案放在專案跟目錄下的 uploads/profile-picture 資料夾中
    //private final String uploadDir = "uploads/profile-picture";

    // 將上傳的檔案放在前端專案的 public 資料夾中, 這樣前端就可以直接訪問圖片
    private final String uploadDir = "/codeRepo/E-Bank-react/public/profile-picture/";

    @Override
    public User getCurrentLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new NotFoundException("請先登入");
        }

        String email = authentication.getName();
        return userRepo.findByEmail(email).orElseThrow(() -> new NotFoundException("用戶不存在"));
    }

    @Override
    public Response<UserDTO> getMyProfile() {
        User user = getCurrentLoggedInUser();

        UserDTO userDTO = modelMapper.map(user, UserDTO.class);

        if (userDTO.getAccounts() != null) {
            userDTO.getAccounts().forEach(accountDTO -> {
                if (accountDTO.getAccountType() != null) {
                    accountDTO.setAccountTypeName(accountDTO.getAccountType().getChinese());
                }

                if (accountDTO.getAccountStatus() != null) {
                    accountDTO.setAccountStatusName(accountDTO.getAccountStatus().getChinese());
                }

                if (accountDTO.getTransactions() != null) {
                    accountDTO.getTransactions().forEach(transactionDTO -> {
                        if (transactionDTO.getTransactionType() != null) {
                            transactionDTO.setTransactionTypeName(transactionDTO.getTransactionType().getChinese());
                        }
                    });
                }
            });
        }

        return Response.<UserDTO>builder()
                .statusCode(HttpStatusCode.OK)
                .message("成功獲取用戶資料")
                .data(userDTO)
                .build();
    }

    @Override
    public Response<Page<UserDTO>> getAllUsers(int page, int size) {
        Page<User> userPage = userRepo.findAll(PageRequest.of(page, size));
        Page<UserDTO> userDTOs = userPage.map(user -> modelMapper.map(user, UserDTO.class));

        return Response.<Page<UserDTO>>builder()
                .statusCode(HttpStatusCode.OK)
                .message("成功獲取所有用戶")
                .data(userDTOs)
                .build();
    }

    @Override
    public Response<?> updatePassword(UpdatePasswordRequest updatePasswordRequest) {
        User user = getCurrentLoggedInUser();

        String oldPassword = updatePasswordRequest.getOldPassword();
        String newPassword = updatePasswordRequest.getNewPassword();

        if (oldPassword == null || newPassword == null) {
            throw new BadRequestException("舊密碼和新密碼不能為空");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BadRequestException("舊密碼不正確");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepo.save(user);

        Map<String, Object> templateVariables = Map.of(
                "name", user.getFirstName()
        );

        NotificationDTO notificationDTO = NotificationDTO.builder()
                .subject("密碼已更新")
                .recipient(user.getEmail())
                .templateName("password-changed")
                .templateVariables(templateVariables)
                .build();

        notificationService.sendEmail(notificationDTO, user);

        return Response.builder()
                .statusCode(HttpStatusCode.OK)
                .message("密碼更新成功")
                .build();
    }

    @Override
    public Response<?> uploadProfilePicture(MultipartFile file) {
        User user = getCurrentLoggedInUser();
        try {

            Path uploadPath = Path.of(uploadDir);

            // 路徑不存在及建立
            if (Files.notExists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 已經存在照片則將舊照片刪除
            String oldProfilePicture = user.getProfilePictureUrl();
            if (oldProfilePicture != null && !oldProfilePicture.isEmpty()) {
                Path oldFile = Paths.get(oldProfilePicture);
                Files.deleteIfExists(oldFile);
            }

            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID() + fileExtension;
            Path filePath = uploadPath.resolve(newFilename);

            Files.copy(file.getInputStream(), filePath);

            user.setProfilePictureUrl("/profile-picture/" + newFilename);
            userRepo.save(user);

            return Response.builder()
                    .statusCode(HttpStatusCode.OK)
                    .message("成功上傳頭像")
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
