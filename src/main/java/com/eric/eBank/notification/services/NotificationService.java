package com.eric.eBank.notification.services;

import com.eric.eBank.auth_users.entity.User;
import com.eric.eBank.notification.dtos.NotificationDTO;

public interface NotificationService {
    void sendEmail(NotificationDTO notificationDTO, User user);
}
