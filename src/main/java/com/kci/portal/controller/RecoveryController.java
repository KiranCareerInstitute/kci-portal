package com.kci.portal.controller;

import com.kci.portal.model.User;
import com.kci.portal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class RecoveryController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    // Show the account recovery form
    @GetMapping("/recover-account")
    public String showRecoveryForm() {
        return "recover-account";
    }

    // Handle form submission to find user
    @PostMapping("/recover-account")
    public String handleRecovery(
            @RequestParam String keyword,
            Model model
    ) {
        List<User> users = userRepository.findByFullNameContainingIgnoreCaseOrMobileContainingIgnoreCase(keyword, keyword);
        model.addAttribute("results", users);
        model.addAttribute("keyword", keyword);
        return "recover-account";
    }

    // Show reset password form
    @GetMapping("/reset-password/{id}")
    public String showResetForm(@PathVariable Long id, Model model) {
        model.addAttribute("userId", id);
        return "reset-password";
    }

    // Submit new password
    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam Long userId,
            @RequestParam String newPassword,
            Model model
    ) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            model.addAttribute("error", "User not found");
            return "reset-password";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        model.addAttribute("success", "Password reset successfully.");
        return "reset-password";
    }
}
