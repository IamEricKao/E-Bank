package com.eric.eBank;

import com.eric.eBank.auth_users.entity.User;
import com.eric.eBank.notification.dtos.NotificationDTO;
import com.eric.eBank.notification.entity.Notification;
import com.eric.eBank.notification.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@RequiredArgsConstructor
public class EBankApplication {

	private final NotificationService notificationService;

	public static void main(String[] args) {
		SpringApplication.run(EBankApplication.class, args);
	}

//	@Bean
//    CommandLineRunner runner(){
//		return args -> {
//            NotificationDTO notificationDTO = NotificationDTO.builder()
//					.recipient("eric40518@yahoo.com.tw")
//					.subject("Test Notification")
//					.body("This is a test notification.")
//					.build();
//            notificationService.sendEmail(notificationDTO, new User());
//        };
//    }
}
