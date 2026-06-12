package com.eric.eBank.notification.dtos;

import com.eric.eBank.auth_users.entity.User;
import com.eric.eBank.enums.NotificationType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationDTO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String subject;

    @NotBlank(message = "不得空白")
    private String recipient;

    private String body;

    private NotificationType type;

    private User user;

    private LocalDateTime createdAt;

    private String templateName;
    // email template 會使用到的變數(資料)
    private Map<String, Object> templateVariables;

}
