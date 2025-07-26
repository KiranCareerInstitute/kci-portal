package com.kci.portal.controller;

import com.kci.portal.model.User;
import com.kci.portal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/recover")
@PreAuthorize("hasRole('ADMIN')")
public class AccountRecoveryController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public String showRecoveryPage() {
        return "admin-account-recovery";
    }

    @PostMapping("/search")
    public String searchUser(@RequestParam("keyword") String keyword, Model model) {
        List<User> users = userRepository
                .findByFullNameContainingIgnoreCaseOrMobileContainingIgnoreCase(keyword, keyword);
        model.addAttribute("users", users);
        model.addAttribute("keyword", keyword);
        return "admin-account-recovery";
    }

    @GetMapping("/reset/{id}")
    public String showResetForm(@PathVariable Long id, Model model) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            model.addAttribute("user", userOpt.get());
            return "admin-reset-password";
        } else {
            return "redirect:/admin/recover?error=notfound";
        }
    }

    @PostMapping("/reset/{id}")
    public String resetPassword(@PathVariable Long id,
                                @RequestParam("password") String password,
                                RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(new BCryptPasswordEncoder().encode(password));
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("message", "Password reset successfully.");
        } else {
            redirectAttributes.addFlashAttribute("error", "User not found.");
        }
        return "redirect:/admin/recover";
    }
}
