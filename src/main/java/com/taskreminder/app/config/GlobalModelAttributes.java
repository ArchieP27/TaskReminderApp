package com.taskreminder.app.config;

import com.taskreminder.app.entity.User;
import com.taskreminder.app.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    @Autowired
    private UserService userService;

    @ModelAttribute("user")
    public User addUserToModel(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) return null;

        return userService.getUserById(userId).orElse(null);
    }

    @ModelAttribute("profileImageVersion")
    public long profileImageVersion() {
        return System.currentTimeMillis();
    }

}
