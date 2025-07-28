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

    @Value("${upload.path.test_submissions}")
    private String testSubmissionsDir;

    private final TestRepository testRepository;
    private final TestSubmissionRepository testSubmissionRepository;
    private final UserRepository userRepository;
    private final UserService userService;      // ← Add this

    @Autowired
    public StudentTestController(TestRepository testRepository,
                                 TestSubmissionRepository testSubmissionRepository,
                                 UserRepository userRepository,
                                 UserService userService) {
        this.testRepository           = testRepository;
        this.testSubmissionRepository = testSubmissionRepository;
        this.userRepository           = userRepository;
        this.userService              = userService;
    }

    // ─── STUDENT VIEWS ───────────────────────────────────────────────────────────────

    @GetMapping("/tests")
    public String showAvailableTests(Model model) {
        model.addAttribute("tests", testRepository.findAll());
        return "student-tests";
    }

    @GetMapping("/tests/submit/{id}")
    public String showSubmitForm(@PathVariable Long id,
                                 Model model,
                                 RedirectAttributes ra) {
        return testRepository.findById(id)
                .map(test -> {
                    model.addAttribute("test", test);
                    return "student-submit-test";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("error", "Test not found.");
                    return "redirect:/student/tests";
                });
    }

    @PostMapping("/tests/submit/{id}")
    public String handleTestSubmission(@PathVariable Long id,
                                       @RequestParam("answerFile") MultipartFile file,
                                       Principal principal,
                                       RedirectAttributes ra) {

        if (file.isEmpty()) {
            ra.addFlashAttribute("error", "Please upload a valid PDF file.");
            return "redirect:/student/tests/submit/" + id;
        }

        Optional<Test> testOpt = testRepository.findById(id);
        Optional<User> userOpt = userRepository.findByEmail(principal.getName());

        if (testOpt.isEmpty() || userOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Test or user not found.");
            return "redirect:/student/tests";
        }

        try {
            // ← here: no extra "test_submissions" folder
            Path submissionDir = Paths.get(testSubmissionsDir)
                    .toAbsolutePath()
                    .normalize();
            Files.createDirectories(submissionDir);

            String fileName = UUID.randomUUID()
                    + "_" + file.getOriginalFilename();
            Path submissionPath = submissionDir.resolve(fileName);
            file.transferTo(submissionPath.toFile());

            TestSubmission submission = new TestSubmission();
            submission.setTest(testOpt.get());
            submission.setUser(userOpt.get());
            submission.setFileName(fileName);
            submission.setSubmittedAt(LocalDateTime.now());
            testSubmissionRepository.save(submission);

            ra.addFlashAttribute("success", "Test submitted successfully.");
            return "redirect:/student/tests";

        } catch (Exception e) {
            e.printStackTrace();
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/student/tests/submit/" + id;
        }
    }

    @GetMapping("/tests/submissions")
    public String viewMySubmissions(Model model,
                                    Principal principal,
                                    RedirectAttributes ra) {
        Optional<User> u = userRepository.findByEmail(principal.getName());
        if (u.isEmpty()) {
            ra.addFlashAttribute("error", "User not found.");
            return "redirect:/student/tests";
        }
        List<TestSubmission> subs = testSubmissionRepository.findByUser(u.get());
        model.addAttribute("submissions", subs);
        return "student-test-submissions";
    }

    @GetMapping("/my-results")
    public String viewMyResults(Model model,
                                Principal principal,
                                RedirectAttributes ra) {
        Optional<User> u = userRepository.findByEmail(principal.getName());
        if (u.isEmpty()) {
            ra.addFlashAttribute("error", "User not found.");
            return "redirect:/student/tests";
        }
        List<TestSubmission> subs = testSubmissionRepository.findByUser(u.get());
        model.addAttribute("submissions", subs);
        return "student-my-results";
    }

    @GetMapping("/results-json")
    @ResponseBody
    public ResponseEntity<List<TestSubmission>> getMyResultsJson(Principal principal) {
        Optional<User> u = userRepository.findByEmail(principal.getName());
        return u
                .map(user -> ResponseEntity.ok(testSubmissionRepository.findByUser(user)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ─── DOWNLOAD YOUR OWN ANSWER ────────────────────────────────────────────────────

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadSubmission(@PathVariable Long id) {
        Optional<TestSubmission> opt = testSubmissionRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TestSubmission submission = opt.get();
        Path filePath = Paths.get(testSubmissionsDir)
                .resolve(submission.getFileName())
                .normalize();

        try {
            Resource res = new UrlResource(filePath.toUri());
            if (!res.exists() || !res.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + submission.getFileName() + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(res);

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─── SERVE TEST‐PAPERS UPLOADED BY ADMIN/TUTOR ─────────────────────────────────

    @GetMapping("/test-pdfs/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveTestPdf(@PathVariable String filename) {
        try {
            // if you still want a subfolder for test‐papers, change this accordingly
            Path filePath = Paths.get(testSubmissionsDir)
                    .resolve(filename)
                    .normalize();
            Resource res = new UrlResource(filePath.toUri());
            if (!res.exists() || !res.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + res.getFilename() + "\"")
                    .body(res);

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    // in StudentTestController
    @PostMapping("/tests/feedback/{submissionId}")
    public String submitTestFeedback(@PathVariable Long submissionId,
                                     @RequestParam String studentFeedback,
                                     Principal principal,
                                     RedirectAttributes ra) {
        Optional<TestSubmission> opt = testSubmissionRepository.findById(submissionId);
        if (opt.isEmpty() || !opt.get().getUser().getEmail().equals(principal.getName())) {
            ra.addFlashAttribute("error", "Cannot leave feedback.");
            return "redirect:/student/tests/submissions";
        }
        TestSubmission sub = opt.get();
        sub.setStudentFeedback(studentFeedback);
        testSubmissionRepository.save(sub);
        ra.addFlashAttribute("success", "Thanks for your feedback!");
        return "redirect:/student/tests/submissions";
    }
}
