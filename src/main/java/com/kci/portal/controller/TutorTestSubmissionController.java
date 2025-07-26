package com.kci.portal.controller;

import com.kci.portal.model.TestSubmission;
import com.kci.portal.repository.TestRepository;
import com.kci.portal.repository.TestSubmissionRepository;
import com.kci.portal.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.kci.portal.model.Test;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import com.kci.portal.model.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
public class TutorTestSubmissionController {

    @Autowired
    private TestSubmissionRepository testSubmissionRepository;

    @Value("${upload.path.tests}")
    private String testUploadPath;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestRepository testRepository;

    /**
     * List all test submissions for tutors.
     */
    @GetMapping("/tutor/test-submissions")
    @PreAuthorize("hasRole('TUTOR')")
    public String viewAllTestSubmissions(Model model) {
        List<TestSubmission> submissions = testSubmissionRepository.findAll();
        model.addAttribute("submissions", submissions);
        return "tutor-view-submissions";
    }

    /**
     * Form for reviewing a test submission.
     */
    @GetMapping("/tutor/test-submissions/review/{id}")
    @PreAuthorize("hasRole('TUTOR')")
    public String showReviewForm(@PathVariable Long id, Model model) {
        TestSubmission submission = testSubmissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid submission ID: " + id));
        model.addAttribute("submission", submission);
        return "tutor-review-submission";
    }

    /**
     * Submit a review (marks and feedback) for a test submission.
     */
    @PostMapping("/tutor/test-submissions/review/{id}")
    @PreAuthorize("hasRole('TUTOR')")
    public String submitReview(
            @PathVariable Long id,
            @RequestParam("marks") Integer marks,
            @RequestParam("feedback") String feedback,
            RedirectAttributes redirectAttributes) {

        TestSubmission submission = testSubmissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid submission ID: " + id));
        submission.setMarks(marks);
        submission.setFeedback(feedback);
        testSubmissionRepository.save(submission);

        redirectAttributes.addFlashAttribute("success", "Review submitted successfully!");
        return "redirect:/tutor/test-submissions/review/" + id;
    }

    /**
     * Download the submitted test file as PDF.
     */
    @GetMapping("/tutor/test-submissions/download/{id}")
    @PreAuthorize("hasRole('TUTOR')")
    public void downloadTestSubmission(
            @PathVariable Long id,
            HttpServletResponse response) throws IOException {
        Optional<TestSubmission> optional = testSubmissionRepository.findById(id);
        if (optional.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Submission not found");
            return;
        }

        TestSubmission submission = optional.get();
        String fileName = submission.getFileName();
        if (fileName == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not available");
            return;
        }

        Path filePath = Paths.get(testUploadPath, "test_submissions", fileName);
        if (!Files.exists(filePath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found on server");
            return;
        }

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        Files.copy(filePath, response.getOutputStream());
        response.getOutputStream().flush();
    }

    /**
     * Filter/search test submissions by status or keyword.
     */
    @GetMapping("/tutor/submissions")
    @PreAuthorize("hasRole('TUTOR')")
    public String filterTestSubmissions(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            Model model) {

        List<TestSubmission> submissions;

        if ("pending".equalsIgnoreCase(status)) {
            submissions = testSubmissionRepository.findByMarksIsNull();
        } else if ("reviewed".equalsIgnoreCase(status)) {
            submissions = testSubmissionRepository.findByMarksIsNotNull();
        } else if (keyword != null && !keyword.trim().isEmpty()) {
            submissions = testSubmissionRepository
                    .findByUserFullNameContainingIgnoreCaseOrTestTitleContainingIgnoreCase(keyword, keyword);
        } else {
            submissions = testSubmissionRepository.findAllWithUserAndTest();
        }

        model.addAttribute("submissions", submissions);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        return "tutor-view-submissions";
    }
    @GetMapping("/tutor/tests/upload")
    @PreAuthorize("hasRole('TUTOR')")
    public String showUploadForm(Model model, Authentication authentication) {
        model.addAttribute("test", new Test());
        List<Test> uploadedTests = testRepository.findAllByUploadedBy_Email(authentication.getName());
        model.addAttribute("uploadedTests", uploadedTests);
        return "tutor/tutor-upload-test";
    }

    // Handle form submission
    @PostMapping("/tutor/tests/upload")
    @PreAuthorize("hasRole('TUTOR')")
    public String handleUpload(@ModelAttribute Test test,
                               @RequestParam("file") MultipartFile file,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a PDF file to upload.");
            return "redirect:/tutor/tests/upload";
        }

        try {
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            // Upload directly to uploads/test_submissions/
            Path uploadDir = Paths.get(testUploadPath); // <- NO extra subfolder
            if (!Files.exists(uploadDir)) Files.createDirectories(uploadDir);
            file.transferTo(uploadDir.resolve(fileName));

            // Save just the filename in DB
            test.setFileName(fileName);
            test.setPdfPath(fileName); // Only filename!
            test.setUploadedAt(LocalDateTime.now());

            // Attach tutor (uploader)
            User tutor = userRepository.findByEmail(auth.getName()).orElse(null);
            test.setUploadedBy(tutor);

            testRepository.save(test);
            redirectAttributes.addFlashAttribute("message", "Test uploaded successfully!");
        } catch (IOException ex) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload test.");
            ex.printStackTrace();
        }

        return "redirect:/tutor/tests/upload";
    }
}
