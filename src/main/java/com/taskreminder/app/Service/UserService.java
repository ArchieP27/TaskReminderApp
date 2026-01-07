package com.taskreminder.app.Service;

import com.taskreminder.app.Entity.User;
import com.taskreminder.app.Repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    public String register(User user) {

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return "An account with this email already exists";
        }

        String otp = generateOtp();
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        user.setVerified(false);

        userRepository.save(user);
        sendOtpEmail(user, otp);

        return "Registration successful. Please verify OTP.";
    }

    public String loginUser(String email, String password, HttpSession session) {

        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            return "User not found!";
        }

        User user = optionalUser.get();

        if (!user.isVerified()) {
            return "User is not verified. Please verify OTP first.";
        }

        if (!user.getPassword().equals(password)) {
            return "Invalid password";
        }

        session.setAttribute("loggedInUser", user.getId());
        session.setAttribute("userId", user.getId());
        session.setAttribute("email", user.getEmail());

        return "Login successful";
    }

    public String verifyOtp(String email, String otp) {

        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            return "User not found";
        }

        User user = optionalUser.get();

        if (user.isVerified()) {
            return "User already verified";
        }

        if (user.getOtp() == null) {
            return "OTP not generated";
        }

        if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            return "OTP expired";
        }

        if (!user.getOtp().equals(otp)) {
            return "Invalid OTP";
        }

        user.setVerified(true);
        user.clearOtp();

        userRepository.save(user);

        return "OTP verified successfully";
    }

    public String resendOtp(String email) {

        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            return "User not found";
        }

        User user = optionalUser.get();

        if (user.isVerified()) {
            return "User already verified";
        }

        String otp = generateOtp();
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

        userRepository.save(user);
        sendOtpEmail(user, otp);

        return "OTP resent successfully";
    }

    private String generateOtp() {
        return String.valueOf((int) (Math.random() * 900000) + 100000);
    }

    private void sendOtpEmail(User user, String otp) {

        String body =
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<body style='font-family: Arial; background-color:#f4f4f4; padding:20px;'>" +
                        "  <div style='background:#ffffff; padding:20px; border-radius:8px;'>" +
                        "    <h2>Welcome to Task Reminder Application 🎉</h2>" +
                        "    <p>Hey <strong>" + user.getName() + "</strong>,</p>" +
                        "    <p>Please use the OTP below to continue logging in:</p>" +
                        "    <h1 style='color:#2d89ef; letter-spacing:4px;'>" + otp + "</h1>" +
                        "    <p>This OTP is valid for a limited time.</p>" +
                        "    <p style='font-size:12px; color:#777;'>Do not share this OTP with anyone.</p>" +
                        "  </div>" +
                        "</body>" +
                        "</html>";

        emailService.sendEmail(
                user.getEmail(),
                "OTP Verification",
                body
        );
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

}
