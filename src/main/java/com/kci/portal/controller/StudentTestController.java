package com.kci.portal.controller;

import com.kci.portal.model.Test;
import com.kci.portal.model.TestSubmission;
import com.kci.portal.model.User;
import com.kci.portal.repository.TestRepository;
import com.kci.portal.repository.TestSubmissionRepository;
import com.kci.portal.repository.UserRepository;
import com.kci.portal.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.MalformedURLException;
import java.nio.file.*;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/student")
public class StudentTestController {

    @Value("${upload.path.tests}")
    private String testsUploadPath; // e.g. C:/.../uploads

    private final TestRepository testRepository;
    private final TestSubmissionRepository testSubmissionRepository;
    private final UserRepository userRepository;

    @Autowired
    public StudentTestController(TestRepository testRepository,
                                 TestSubmissionRepository testSubmissionRepository,
                                 UserRepository userRepository,
                                 UserService userService) {
        this.testRepository = testRepository;
        this.testSubmissionRepository = testSubmissionRepository;
        this.userRepository = userRepository;
    }

    // Show all available tests
    @GetMapping("/tests")
    public String showAvailableTests(Model model) {
        model.addAttribute("tests", testRepository.findAll());
        return "student-tests";
    }

    // Show test submission form
    @GetMapping("/tests/submit/{id}")
    public String showSubmitForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return testRepository.findById(id)
                .map(test -> {
                    model.addAttribute("test", test);
                    return "student-submit-test";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Test not found.");
                    return "redirect:/student/tests";
                });
    }

    // Handle student test submission (PDF answer)
    @PostMapping("/tests/submit/{id}")
    public String handleTestSubmission(@PathVariable Long id,
                                       @RequestParam("answerFile") MultipartFile file,
                                       Principal principal,
                                       RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Please upload a valid PDF file.");
                return "redirect:/student/tests/submit/" + id;
            }

            Optional<Test> testOpt = testRepository.findById(id);
            Optional<User> userOpt = userRepository.findByEmail(principal.getName());

            if (testOpt.isEmpty() || userOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Test or User not found.");
                return "redirect:/student/tests";
            }

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path submissionDir = Paths.get(testsUploadPath, "test_submissions");
            Files.createDirectories(submissionDir);
            Path submissionPath = submissionDir.resolve(fileName);
            file.transferTo(submissionPath);

            TestSubmission submission = new TestSubmission();
            submission.setTest(testOpt.get());
            submission.setUser(userOpt.get());
            submission.setFileName(fileName);
            submission.setSubmittedAt(LocalDateTime.now());

            testSubmissionRepository.save(submission);

            redirectAttributes.addFlashAttribute("success", "Test submitted successfully.");
            return "redirect:/student/tests";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/student/tests/submit/" + id;
        }
    }

    // View student's own submissions
    @GetMapping("/tests/submissions")
    public String viewMySubmissions(Model model, Principal principal, RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userRepository.findByEmail(principal.getName());

        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/student/tests";
        }

        List<TestSubmission> submissions = testSubmissionRepository.findByUser(userOpt.get());
        model.addAttribute("submissions", submissions);
        return "student-test-submissions";
    }

    // View marks and feedback (results)
    @GetMapping("/my-results")
    public String viewMyResults(Model model, Principal principal, RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userRepository.findByEmail(principal.getName());

        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/student/tests";
        }

        List<TestSubmission> submissions = testSubmissionRepository.findByUser(userOpt.get());
        model.addAttribute("submissions", submissions);
        return "student-my-results";
    }

    // Optional: JSON API for results
    @GetMapping("/results-json")
    @ResponseBody
    public ResponseEntity<List<TestSubmission>> getMyResultsJson(Principal principal) {
        Optional<User> userOpt = userRepository.findByEmail(principal.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(testSubmissionRepository.findByUser(userOpt.get()));
    }

    // Download a student's own submitted answer (from /uploads/test_submissions)
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadSubmission(@PathVariable Long id) {
        Optional<TestSubmission> optionalSubmission = testSubmissionRepository.findById(id);
        if (optionalSubmission.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TestSubmission submission = optionalSubmission.get();
        Path filePath = Paths.get(testsUploadPath, "test_submissions", submission.getFileName());

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + submission.getFileName() + "\"")
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // **DOWNLOAD TEST PAPER (uploaded by tutor/admin)**
    @GetMapping("/test-pdfs/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveTestPdf(@PathVariable String filename) {
        try {
            // Serve from uploads/test_submissions/test_papers
            Path filePath = Paths.get(testsUploadPath, "test_submissions", "test_papers").resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
