package com.taskreminder.app.service;

import com.taskreminder.app.dto.UpdateProfileRequest;
import com.taskreminder.app.entity.User;
import com.taskreminder.app.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int OTP_RESEND_COOLDOWN_SECONDS = 30;
    private static final int MAX_OTP_ATTEMPTS = 5;
    public String register(User user) {

        if (user.getName() == null || user.getName().isBlank() ||
                user.getEmail() == null || user.getEmail().isBlank() ||
                user.getPassword() == null || user.getPassword().isBlank()) {
            return "All fields are required";
        }

        if (user.getName().length() < 2 || user.getName().length() > 50) {
            return "Name must be between 2 and 50 characters";
        }

        if (!isValidEmail(user.getEmail())) {
            return "Invalid email address";
        }

        if (!isValidPassword(user.getPassword())) {
            return "Password must be at least 8 characters, contain uppercase, lowercase, number, and special character";
        }

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return "An account with this email already exists";
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        String otp = generateOtp();
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        user.setVerified(false);
        user.setOtpAttempts(0);
        user.setOtpRequestedTime(LocalDateTime.now());

        userRepository.save(user);

        try {
            emailService.sendOtpEmail(user, otp);
        } catch (Exception e) {
            return "Registration successful, but OTP email could not be sent. Please try resending OTP.";
        }

        return "Registration successful. Please verify OTP.";
    }

    public String loginUser(String email, String password, HttpSession session) {

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return "Email and password are required";
        }

        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            return "User not found";
        }

        User user = optionalUser.get();

//        if (!user.isVerified()) {
//            return "User is not verified. Please verify OTP first";
//        }

        // BCrypt Password Migration
//        if (user.getPassword().equals(password)) {
//            user.setPassword(passwordEncoder.encode(password));
//            userRepository.save(user);
//        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return "Invalid password";
        }

        session.setAttribute("loggedInUser", user.getId());
        session.setAttribute("userId", user.getId());
        session.setAttribute("email", user.getEmail());
        session.setAttribute("name", user.getName());

        return "Login successful";
    }

    public String verifyOtp(String email, String otp) {

        if (otp == null || otp.isBlank()) {
            return "OTP cannot be empty";
        }

        if (!otp.matches("\\d{6}")) {
            return "Invalid OTP format";
        }

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
            return "OTP expired. Please request a new one";
        }

        if (user.getOtpAttempts() >= MAX_OTP_ATTEMPTS) {
            return "You have exceeded OTP attempts. Please request a new OTP";
        }

        if (!user.getOtp().equals(otp)) {
            user.setOtpAttempts(user.getOtpAttempts() + 1);
            userRepository.save(user);
            return "Incorrect OTP. Please try again";
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
            return "User already verified. No OTP needed";
        }

        if (user.getOtpRequestedTime() != null &&
                user.getOtpRequestedTime().plusSeconds(OTP_RESEND_COOLDOWN_SECONDS).isAfter(LocalDateTime.now())) {
            return "OTP was already sent. Please wait before requesting again";
        }

        String otp = generateOtp();
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        user.setOtpAttempts(0);
        user.setOtpRequestedTime(LocalDateTime.now());

        userRepository.save(user);

        try {
            emailService.sendOtpEmail(user, otp);
        } catch (Exception e) {
            return "OTP could not be sent. Please try again later";
        }

        return "OTP resent successfully";
    }

    private String generateOtp() {
        return String.valueOf((int) (Math.random() * 900000) + 100000);
    }

    public String sendResetOtp(String email) {

        if (email == null || email.isBlank()) {
            return "Email is required";
        }

        if (!isValidEmail(email)) {
            return "Invalid email address";
        }

        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            return "No account found with this email";
        }

        User user = optionalUser.get();

        if (user.getOtpRequestedTime() != null &&
                user.getOtpRequestedTime().plusSeconds(OTP_RESEND_COOLDOWN_SECONDS).isAfter(LocalDateTime.now())) {
            return "OTP was already sent. Please wait before requesting again";
        }

        String otp = generateOtp();
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        user.setOtpRequestedTime(LocalDateTime.now());
        user.setOtpAttempts(0);

        userRepository.save(user);

        try {
            emailService.sendOtpEmail(user, otp);
        } catch (Exception e) {
            return "OTP could not be sent. Please try again later";
        }

        return "OTP sent to your registered email successfully";
    }

    public String verifyResetOtp(String email, String otp, HttpSession session) {

        if (email == null || email.isBlank()) {
            return "Email is required";
        }

        if (otp == null || otp.isBlank()) {
            return "OTP is required";
        }

        if (!otp.matches("\\d{6}")) {
            return "Invalid OTP format";
        }

        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            return "User not found";
        }

        User user = optionalUser.get();

        if (user.getOtp() == null) {
            return "OTP not generated. Please request one first";
        }

        if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            return "OTP expired. Please request again";
        }

        if (user.getOtpAttempts() >= MAX_OTP_ATTEMPTS) {
            return "You have exceeded OTP attempts. Please request a new OTP";
        }

        if (!user.getOtp().equals(otp)) {
            user.setOtpAttempts(user.getOtpAttempts() + 1);
            userRepository.save(user);
            return "Invalid OTP. Please try again";
        }

        session.setAttribute("RESET_OTP_VERIFIED", email);
        user.clearOtp();
        userRepository.save(user);

        return "OTP verified successfully";
    }

    public String resetPassword(
            String email,
            String password,
            String confirmPassword,
            HttpSession session
    ) {

        if (email == null || email.isBlank()) {
            return "Email is required";
        }

        if (password == null || password.isBlank() || confirmPassword == null || confirmPassword.isBlank()) {
            return "Both password fields are required";
        }

        if (!password.equals(confirmPassword)) {
            return "Passwords do not match";
        }

        if (!isValidPassword(password)) {
            return "Password must be at least 8 characters, contain uppercase, lowercase, number, and special character";
        }

        String verifiedEmail = (String) session.getAttribute("RESET_OTP_VERIFIED");
        if (verifiedEmail == null || !verifiedEmail.equals(email)) {
            return "Unauthorized password reset attempt";
        }

        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            return "User not found";
        }

        User user = optionalUser.get();

        if (passwordEncoder.matches(password, user.getPassword())) {
            return "New password cannot be the same as the old password. Please choose a different one";
        }

        user.setPassword(passwordEncoder.encode(password));
        user.clearOtp();

        userRepository.save(user);

        session.removeAttribute("RESET_OTP_VERIFIED");

        return "Password reset successful. Please login";
    }


    private boolean isValidEmail(String email) {
        String regex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
        return Pattern.matches(regex, email);
    }

    private boolean isValidPassword(String password) {
        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        return Pattern.matches(regex, password);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> getUserById(Integer userId) {
        return userRepository.findById(userId);
    }

    public void updateProfile(Integer userId, UpdateProfileRequest dto) {

        User user = getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new RuntimeException("Username cannot be empty");
        }
        String name = dto.getName().trim();
        if (name.length() < 3 || name.length() > 30) {
            throw new RuntimeException("Username must be between 3 and 30 characters");
        }
        if (!name.matches("^[a-zA-Z0-9_ ]+$")) {
            throw new RuntimeException("Username contains invalid characters");
        }

        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email cannot be empty");
        }
        String email = dto.getEmail().trim().toLowerCase();
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new RuntimeException("Invalid email format");
        }

        boolean emailChanged = !email.equalsIgnoreCase(user.getEmail());

        user.setName(name);
        user.setEmail(email);

        if (emailChanged) {
            user.setVerified(false);
        }

        if (dto.getProfileImage() != null && !dto.getProfileImage().isEmpty()) {
            long maxSize = 2 * 1024 * 1024;
            if (dto.getProfileImage().getSize() > maxSize) {
                throw new RuntimeException("Profile image must be less than 2MB");
            }

            String contentType = dto.getProfileImage().getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new RuntimeException("Only image files are allowed");
            }

            String uploadDir = "uploads/profiles/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            String filename = "user_" + userId + ".jpg";
            Path filePath = Paths.get(uploadDir, filename);

            try {
                Files.write(filePath, dto.getProfileImage().getBytes());
                user.setProfileImage("/uploads/profiles/" + filename);
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload profile image");
            }
        }
        userRepository.save(user);
    }


}
