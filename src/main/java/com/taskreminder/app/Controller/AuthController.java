package com.taskreminder.app.Controller;

import com.taskreminder.app.Entity.User;
import com.taskreminder.app.Repository.UserRepository;
import com.taskreminder.app.Service.UserService;
import com.taskreminder.app.dto.LoginRequest;
import com.taskreminder.app.dto.OtpRequest;
import com.taskreminder.app.dto.RegisterRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @GetMapping("/register")
    public String showRegister(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @ModelAttribute RegisterRequest request,
            RedirectAttributes redirectAttributes
    ) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Passwords do not match"
            );
            return "redirect:/auth/register";
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());

        String response = userService.register(user);

        if (response.startsWith("Registration successful")) {
            redirectAttributes.addFlashAttribute("successMessage", response);
            return "redirect:/auth/verify-otp?email=" + user.getEmail();
        }

        redirectAttributes.addFlashAttribute("errorMessage", response);
        return "redirect:/auth/register";
    }

    @GetMapping("/verify-otp")
    public String showOtpPage(
            @RequestParam(required = false) String email,
            Model model
    ) {
        model.addAttribute("email", email);
        OtpRequest otpRequest = new OtpRequest();
        if (email != null) {
            Optional<User> userOpt = userService.findByEmail(email);
            otpRequest.setEmail(email);
            if (userOpt.isPresent() && userOpt.get().getOtpExpiry() != null) {
                long expiryMillis = userOpt.get().getOtpExpiry()
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();
                model.addAttribute("otpExpiryMillis", expiryMillis);
            }
        }
        model.addAttribute("otpRequest", otpRequest);
        return "verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtpMvc(
            @ModelAttribute("otpRequest") OtpRequest otpRequest,
            RedirectAttributes redirectAttributes
    ) {
        String response = userService.verifyOtp(otpRequest.getEmail(), otpRequest.getOtp());

        if (response.contains("successfully")) {
            redirectAttributes.addFlashAttribute("successMessage", response);
            return "redirect:/auth/login";
        }

        redirectAttributes.addFlashAttribute("errorMessage", response);
        return "redirect:/auth/verify-otp?email=" + otpRequest.getEmail();
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String loginMvc(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String response = userService.loginUser(email, password, session);

        if (response.equalsIgnoreCase("Login successful")) {
            return "redirect:/api/tasks";
        }

        redirectAttributes.addFlashAttribute("errorMessage", response);
        return "redirect:/auth/login";
    }

    @PostMapping("/resend-otp")
    public String resendOtp(
            @RequestParam String email,
            RedirectAttributes redirectAttributes
    ) {
        String response = userService.resendOtp(email);

        if (response.contains("successfully")) {
            redirectAttributes.addFlashAttribute("successMessage", response);
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", response);
        }

        return "redirect:/auth/verify-otp?email=" + email;
    }

    @GetMapping("/logout")
    public String logoutMvc(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("successMessage", "Logged out successfully.");
        return "redirect:/auth/login";
    }


    @PostMapping("/api/verify")
    @ResponseBody
    public ResponseEntity<?> verifyApi(@RequestBody OtpRequest request) {
        String response = userService.verifyOtp(
                request.getEmail(),
                request.getOtp()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/login")
    @ResponseBody
    public ResponseEntity<?> loginApi(
            @RequestBody LoginRequest request,
            HttpSession session
    ) {
        String response = userService.loginUser(
                request.getEmail(),
                request.getPassword(),
                session
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/me")
    @ResponseBody
    public ResponseEntity<?> me(HttpSession session) {

        Integer userId = (Integer) session.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.ok(Map.of("loggedIn", false));
        }

        return ResponseEntity.ok(
                Map.of(
                        "loggedIn", true,
                        "id", userId,
                        "email", session.getAttribute("email")
                )
        );
    }

    @PostMapping("/api/logout")
    @ResponseBody
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("Logged out successfully");
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(
            @RequestParam String email,
            Model model
    ) {
        String result = userService.sendResetOtp(email);

        if (!result.startsWith("OTP sent")) {
            model.addAttribute("errorMessage", result);
            return "forgot-password";
        }

        model.addAttribute("successMessage", result);
        model.addAttribute("email", email);
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isPresent() && userOpt.get().getOtpExpiry() != null) {
            long expiryMillis = userOpt.get().getOtpExpiry()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
            model.addAttribute("otpExpiryMillis", expiryMillis);
        }

        return "verify-reset-otp";
    }
    @PostMapping("/verify-reset-otp")
    public String verifyResetOtp(
            @RequestParam String email,
            @RequestParam String otp,
            HttpSession session,
            Model model
    ) {
        String result = userService.verifyResetOtp(email, otp, session);

        if (!result.contains("successfully")) {
            model.addAttribute("errorMessage", result);
            model.addAttribute("email", email);
            return "verify-reset-otp";
        }

        return "redirect:/auth/reset-password?email=" + email;
    }

    @GetMapping("/reset-password")
    public String showResetPassword(@RequestParam String email, Model model) {
        model.addAttribute("email", email);
        return "reset-password";
    }

    @PostMapping("/resend-reset-otp")
    public String resendResetOtp(
            @RequestParam String email,
            Model model
    ) {
        String response = userService.sendResetOtp(email);

        if (!response.startsWith("OTP sent")) {
            model.addAttribute("errorMessage", response);
        } else {
            model.addAttribute("successMessage", response);
        }

        model.addAttribute("email", email);

        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isPresent() && userOpt.get().getOtpExpiry() != null) {
            long expiryMillis = userOpt.get().getOtpExpiry()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
            model.addAttribute("otpExpiryMillis", expiryMillis);
        }

        return "verify-reset-otp";
    }

    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            HttpSession session,
            Model model
    ) {
        String result = userService.resetPassword(email, password, confirmPassword, session);

        if (!result.startsWith("Password reset successful")) {
            model.addAttribute("errorMessage", result);
            model.addAttribute("email", email);
            return "reset-password";
        }

        model.addAttribute("successMessage", result);
        return "login";
    }

}
