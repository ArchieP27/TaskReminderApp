package com.taskreminder.app.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private boolean verified = false;

    private String otp;
    private LocalDateTime otpExpiry;
    private Integer otpAttempts = 0;
    private LocalDateTime otpRequestedTime;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "profile_image")
    private String profileImage;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.profileImage == null) {
            this.profileImage = "/images/profile.png";
        }
    }

    public void clearOtp() {
        this.otp = null;
        this.otpExpiry = null;
        this.otpAttempts=0;
        this.otpRequestedTime=null;
    }

    public User(){}

    public User(Integer id, String name, String email, String password, boolean verified, String otp, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.verified = verified;
        this.otp=otp;
        this.createdAt = createdAt;
        this.otpAttempts=0;
        this.otpRequestedTime=null;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getOtp(){
        return otp;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getOtpExpiry(){
        return otpExpiry;
    }

    public void setOtpExpiry(LocalDateTime otpExpiry){
        this.otpExpiry = otpExpiry;
    }

    public Integer getOtpAttempts() {
        return otpAttempts;
    }
    public void setOtpAttempts(Integer otpAttempts) {
        this.otpAttempts = otpAttempts;
    }

    public LocalDateTime getOtpRequestedTime() {
        return otpRequestedTime;
    }
    public void setOtpRequestedTime(LocalDateTime otpRequestedTime) {
        this.otpRequestedTime = otpRequestedTime;
    }

}