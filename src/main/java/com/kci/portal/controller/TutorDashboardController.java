package com.kci.portal.controller;

import com.kci.portal.model.TestSubmission;
import com.kci.portal.model.User;
import com.kci.portal.repository.StudentQueryRepository;
import com.kci.portal.repository.TestSubmissionRepository;
import com.kci.portal.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
@PreAuthorize("hasRole('TUTOR')")
public class TutorDashboardController {

    @Autowired
    private TestSubmissionRepository testSubmissionRepository;

    @Autowired
    private StudentQueryRepository studentQueryRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/tutor/dashboard")
    public String showTutorDashboard(Model model, Principal principal) {

        // ✅ Get logged-in tutor info
        String email = principal.getName();
        User tutor = userRepository.findByEmail(email).orElse(null);

        // ✅ Count test submissions
        long testSubmissionCount = testSubmissionRepository.count();

        // ✅ Count total queries
        long queryCount = studentQueryRepository.count();

        // ✅ Count total students (users with role STUDENT)
        long studentCount = userRepository.findByRole("ROLE_STUDENT").size();

        // ✅ Add to model
        model.addAttribute("tutor", tutor);
        model.addAttribute("testSubmissionCount", testSubmissionCount);
        model.addAttribute("queryCount", queryCount);
        model.addAttribute("studentCount", studentCount);

        return "tutor-dashboard";
    }

    @GetMapping("/tutor/download/{id}")
    public void downloadSubmissionFile(@PathVariable Long id, HttpServletResponse response) throws IOException {
        var submission = testSubmissionRepository.findById(id).orElse(null);
        if (submission != null && submission.getFileName() != null) {
            Path file = Paths.get("uploads/" + submission.getFileName()); // adjust path if needed
            if (Files.exists(file)) {
                response.setContentType("application/octet-stream");
                response.setHeader("Content-Disposition", "attachment; filename=" + file.getFileName());
                Files.copy(file, response.getOutputStream());
                response.getOutputStream().flush();
            }
        }
    }
}
