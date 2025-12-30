package com.taskreminder.app;

import com.taskreminder.app.Service.EmailService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class AppApplication {

	public static void main(String[] args) {

		SpringApplication.run(AppApplication.class, args);

//		ApplicationContext context = (ApplicationContext) SpringApplication.run(AppApplication.class, args);
//
//		EmailService emailService = context.getBean(EmailService.class);
//		emailService.sendEmail("2708archita@gmail.com","Test Mail","Hey, this is a test mail");
//
	}

}
