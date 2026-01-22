package com.taskreminder.app.service;

import com.taskreminder.app.dto.UpdateProfileRequest;
import com.taskreminder.app.entity.User;
import com.taskreminder.app.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private HttpSession session;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(1);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPassword("Test@1234");
        user.setVerified(false);
    }

    @Test
    void testRegisterSuccess() {
        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(any()))
                .thenReturn("ENCODED");

        String result = userService.register(user);

        assertEquals("Registration successful. Please verify OTP.", result);
        verify(userRepository).save(user);
        verify(emailService).sendOtpEmail(eq(user), any());
    }

    @Test
    void testRegisterDuplicateEmail() {
        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        String result = userService.register(user);

        assertEquals("An account with this email already exists", result);
    }

    @Test
    void testLoginSuccess() {
        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "Test@1234"))
                .thenReturn(true);

        String result = userService.loginUser(user.getEmail(), "pass", session);

        assertEquals("Login successful", result);
        verify(session).setAttribute(eq("userId"), eq(1));
    }

    @Test
    void testLoginInvalidPassword() {
        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any()))
                .thenReturn(false);

        String result = userService.loginUser(user.getEmail(), "wrong", session);

        assertEquals("Invalid password", result);
    }

    @Test
    void testVerifyOtpSuccess() {
        user.setOtp("123456");
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        String result = userService.verifyOtp(user.getEmail(), "123456");

        assertEquals("OTP verified successfully", result);
        assertTrue(user.isVerified());
        verify(userRepository).save(user);
    }

    @Test
    void testVerifyOtpExpired() {
        user.setOtp("123456");
        user.setOtpExpiry(LocalDateTime.now().minusMinutes(1));

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        String result = userService.verifyOtp(user.getEmail(), "123456");

        assertEquals("OTP expired. Please request a new one", result);
    }

    @Test
    void testResendOtpSuccess() {
        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        String result = userService.resendOtp(user.getEmail());

        assertEquals("OTP resent successfully", result);
        verify(emailService).sendOtpEmail(eq(user), any());
    }

    @Test
    void testSendResetOtpSuccess() {
        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        String result = userService.sendResetOtp(user.getEmail());

        assertEquals("OTP sent to your registered email successfully", result);
    }

    @Test
    void testVerifyResetOtpSuccess() {
        user.setOtp("123456");
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        String result = userService.verifyResetOtp(user.getEmail(), "123456", session);

        assertEquals("OTP verified successfully", result);
        verify(session).setAttribute("RESET_OTP_VERIFIED", user.getEmail());
    }

    @Test
    void testResetPasswordSuccess() {
        when(session.getAttribute("RESET_OTP_VERIFIED"))
                .thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any()))
                .thenReturn(false);
        when(passwordEncoder.encode(any()))
                .thenReturn("NEWPASS");

        String result = userService.resetPassword(
                user.getEmail(),
                "NewPass@123",
                "NewPass@123",
                session
        );

        assertEquals("Password reset successful. Please login", result);
        verify(userRepository).save(user);
    }

    @Test
    void testUpdateProfileSuccess() {
        UpdateProfileRequest dto = new UpdateProfileRequest();
        dto.setName("Updated User");
        dto.setEmail("updated@example.com");

        when(userRepository.findById(1))
                .thenReturn(Optional.of(user));

        userService.updateProfile(1, dto);

        assertEquals("Updated User", user.getName());
        assertEquals("updated@example.com", user.getEmail());
        verify(userRepository).save(user);
    }

    @Test
    void testUpdateProfileUserNotFound() {
        when(userRepository.findById(1))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> userService.updateProfile(1, new UpdateProfileRequest())
        );

        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void testFindByEmail() {
        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        Optional<User> result = userService.findByEmail(user.getEmail());

        assertTrue(result.isPresent());
    }

    @Test
    void testGetEmailByUserId() {
        when(userRepository.findEmailByUserId(1))
                .thenReturn(Optional.of(user.getEmail()));

        String email = userService.getEmailByUserId(1);

        assertEquals(user.getEmail(), email);
    }
}
