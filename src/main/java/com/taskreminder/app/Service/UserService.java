package com.taskreminder.app.Service;

import com.taskreminder.app.Entity.User;
import com.taskreminder.app.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

//    @Autowired
//    private BCryptPasswordEncoder passwordEncoder;

    public User register(User user){
        if(userRepository.findByEmail(user.getEmail()).isPresent())
            throw new RuntimeException("An account with the given email already exists!");
//        user.setPassword(passwordEncoder.encode(user.getPassword()));
        String otp = generateOtp();
        user.setOtp(otp);
        user.setVerified(false);
        System.out.println("OTP for " + user.getEmail() + " = " + otp);
        return userRepository.save(user);
    }

    private String generateOtp() {
        int otp = (int) (Math.random() * 900000) + 100000;
        return String.valueOf(otp);
    }

    public void verifyOtp(String email, String otp) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getOtp().equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }

        user.setVerified(true);
        user.setOtp(null);

        userRepository.save(user);
    }

    public void login(String email, String rawPassword) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

//        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
//            throw new RuntimeException("Invalid email or password");
//        }
        if (!user.getPassword().equals(rawPassword)) {
            throw new RuntimeException("Invalid email or password");
        }

        if (!user.isVerified()) {
            throw new RuntimeException("Please verify your account first");
        }
    }

    public void resendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if(user.isVerified()) {
            throw new RuntimeException("Account is already verified");
        }

        String otp = generateOtp();
        user.setOtp(otp);
        userRepository.save(user);

        System.out.println("Resent OTP for " + email + " = " + otp);
    }

    public User findByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

}
