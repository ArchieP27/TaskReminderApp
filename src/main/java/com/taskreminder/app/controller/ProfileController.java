package com.taskreminder.app.controller;

import com.taskreminder.app.dto.UpdateProfileRequest;
import com.taskreminder.app.entity.User;
import com.taskreminder.app.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;

@Controller
public class ProfileController {

    @Autowired
    private UserService userService;

    @GetMapping("/profile")
    public String viewProfile(Model model, HttpSession session) {

        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        User user = userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        model.addAttribute("user", user);
        return "profile";
    }

    @GetMapping("/profile/edit")
    public String editProfile(Model model, HttpSession session) {

        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        User user = userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UpdateProfileRequest dto = new UpdateProfileRequest();
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());


        long profileImageVersion = System.currentTimeMillis();
        model.addAttribute("profileImageVersion", profileImageVersion);
        if (!user.isVerified() && user.getOtpExpiry() != null) {
            model.addAttribute(
                    "otpExpiryMillis",
                    user.getOtpExpiry()
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
            );
        }

        model.addAttribute("otpSent", false);
        model.addAttribute("profile", dto);
        model.addAttribute("user", user);

        return "profile-edit";
    }

    @PostMapping("/profile/edit")
    public String updateProfile(
            @ModelAttribute("profile") UpdateProfileRequest dto,
            HttpSession session,
            RedirectAttributes ra
    ) {

        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        if (dto.getName() == null || dto.getName().trim().length() < 3) {
            ra.addFlashAttribute("errorMessage", "Name must be at least 3 characters long");
            ra.addFlashAttribute("profile", dto);
            return "redirect:/profile/edit";
        }

        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            ra.addFlashAttribute("errorMessage", "Email cannot be empty");
            ra.addFlashAttribute("profile", dto);
            return "redirect:/profile/edit";
        }

        if (!dto.getEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            ra.addFlashAttribute("errorMessage", "Invalid email format");
            ra.addFlashAttribute("profile", dto);
            return "redirect:/profile/edit";
        }

        try {
            userService.updateProfile(userId, dto);
            ra.addFlashAttribute("successMessage", "Profile updated successfully!");
            return "redirect:/profile/edit";
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            ra.addFlashAttribute("profile", dto);
            return "redirect:/profile/edit";
        }
    }

    @PostMapping("/profile/send-otp")
    public String sendProfileOtp(HttpSession session, RedirectAttributes ra) {

        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        User user = userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isVerified()) {
            ra.addFlashAttribute("errorMessage", "Email is already verified");
            return "redirect:/profile/edit";
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            ra.addFlashAttribute("errorMessage", "Email not found for OTP");
            return "redirect:/profile/edit";
        }

        String result = userService.resendOtp(user.getEmail());

        if (result.toLowerCase().contains("success")) {
            ra.addFlashAttribute("successMessage", result);
            ra.addFlashAttribute("otpSent", true);
        } else {
            ra.addFlashAttribute("errorMessage", result);
        }

        return "redirect:/profile/edit";
    }

    @PostMapping("/profile/verify-otp")
    public String verifyProfileOtp(
            String otp,
            HttpSession session,
            RedirectAttributes ra
    ) {

        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        if (otp == null || otp.trim().length() != 6) {
            ra.addFlashAttribute("errorMessage", "OTP must be 6 digits");
            return "redirect:/profile/edit";
        }

        User user = userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String result = userService.verifyOtp(user.getEmail(), otp);

        if (result.toLowerCase().contains("success")) {
            ra.addFlashAttribute("successMessage", result);
        } else {
            ra.addFlashAttribute("errorMessage", result);
        }

        return "redirect:/profile/edit";
    }

    @GetMapping(
            value = "/profile/image/{fileName}",
            produces = {
                    MediaType.IMAGE_PNG_VALUE,
                    MediaType.IMAGE_JPEG_VALUE
            }
    )
    public ResponseEntity<Resource> serveImage(@PathVariable String fileName) throws IOException {
        Path imagePath = Paths.get("uploads/profile", fileName);
        if (!Files.exists(imagePath)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new UrlResource(imagePath.toUri());
        MediaType mediaType;
        if (fileName.toLowerCase().endsWith(".png")) {
            mediaType = MediaType.IMAGE_PNG;
        } else if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) {
            mediaType = MediaType.IMAGE_JPEG;
        } else {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity
                .ok()
                .contentType(mediaType)
                .body(resource);
    }

    @PostMapping("/profile/delete-image")
    @ResponseBody
    public String deleteProfileImage(HttpSession session) {

        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            throw new RuntimeException("Unauthorized");
        }

        userService.deleteProfileImage(userId);
        return "success";
    }

}
