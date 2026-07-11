package com.eric.eBank.auth_users.services;

import com.eric.eBank.auth_users.dtos.UpdatePasswordRequest;
import com.eric.eBank.auth_users.dtos.UserDTO;
import com.eric.eBank.auth_users.entity.User;
import com.eric.eBank.res.Response;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    User getCurrentLoggedInUser();

    Response<UserDTO> getMyProfile();

    Response<Page<UserDTO>> getAllUsers(int page, int size);

    Response<?> updatePassword(UpdatePasswordRequest updatePasswordRequest);

    Response<?> uploadProfilePicture(MultipartFile file);
}
