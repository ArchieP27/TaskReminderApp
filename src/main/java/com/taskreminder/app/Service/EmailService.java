package com.taskreminder.app.Service;

import com.taskreminder.app.Entity.Task;
import com.taskreminder.app.Entity.User;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Properties;

@Service
public class EmailService {

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private int port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    public void sendEmail(String to, String subject, String body) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        Session session = Session.getInstance(props, getPasswordAuthentication());
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(
                    Message.RecipientType.TO, InternetAddress.parse(to)
            );
            message.setSubject(subject);
            message.setContent(body, "text/html; charset=utf-8");

            Transport.send(message);

        }catch (MessagingException e) {
            throw new RuntimeException("Failed to send email.", e);
        }
    }

    private Authenticator getPasswordAuthentication() {
        return new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication(){
                return new PasswordAuthentication(username,password);
            }
        };
    }

    public void sendOtpEmail(User user, String otp) {
        String subject = "OTP Verification for Task Reminder App";
        String body = buildOtpHtml(user, otp);
        sendEmail(user.getEmail(), subject, body);
    }

    private String buildOtpHtml(User user, String otp) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<body style='font-family: Arial, sans-serif; background-color:#f4f4f4; padding:20px;'>" +
                "  <div style='background:#ffffff; padding:20px; border-radius:8px;'>" +
                "    <h2>Welcome to Task Reminder App 🎉</h2>" +
                "    <p>Hey <strong>" + user.getName() + "</strong>,</p>" +
                "    <p>Please use the OTP below to verify your account:</p>" +
                "    <h1 style='color:#2d89ef; letter-spacing:4px;'>" + otp + "</h1>" +
                "    <p>This OTP is valid for a limited time.</p>" +
                "    <p style='font-size:12px; color:#777;'>Do not share this OTP with anyone.</p>" +
                "  </div>" +
                "</body>" +
                "</html>";
    }

    public void sendTaskReminder(Task task) {
        String subject = "Task Reminder: " + task.getTitle();
        String body = buildTaskReminderHtml(task);
        sendEmail(task.getUser().getEmail(), subject, body);
    }

    private String buildTaskReminderHtml(Task task) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<body style='font-family: Arial, sans-serif; background-color:#f4f4f4; padding:20px;'>" +
                "  <div style='background:#ffffff; padding:20px; border-radius:8px;'>" +
                "    <h2>Task Reminder ⏰</h2>" +
                "    <p>Hi <strong>" + task.getUser().getName() + "</strong>,</p>" +
                "    <p>This is a reminder for your upcoming task:</p>" +
                "    <ul>" +
                "      <li><strong>Title:</strong> " + task.getTitle() + "</li>" +
                "      <li><strong>Due At:</strong> " + dtf.format(task.getReminderTime()) + "</li>" +
                "      <li><strong>Description:</strong> " + task.getDescription() + "</li>" +
                "    </ul>" +
                "    <p style='font-size:12px; color:#777;'>Make sure to complete it on time!</p>" +
                "  </div>" +
                "</body>" +
                "</html>";
    }
}