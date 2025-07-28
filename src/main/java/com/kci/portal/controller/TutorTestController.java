package com.kci.portal.controller;

import com.kci.portal.model.Test;
import com.kci.portal.model.TestSubmission;
import com.kci.portal.model.User;
import com.kci.portal.repository.TestRepository;
import com.kci.portal.repository.TestSubmissionRepository;
import com.kci.portal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/tutor/tests")
@PreAuthorize("hasRole('TUTOR')")
public class TutorTestController {

    @Value("${upload.path.test_submissions}")
    private String testSubmissionsDir;

    private final TestRepository testRepository;
    private final TestSubmissionRepository submissionRepository;
    private final UserRepository userRepository;

    @Autowired
    public TutorTestController(
            TestRepository testRepository,
            TestSubmissionRepository submissionRepository,
            UserRepository userRepository
    ) {
        this.testRepository = testRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
    }

    // 1. List all tests created by this tutor
    @GetMapping
    public String listTests(Model model, Principal principal) {
        String tutorEmail = principal.getName();
        User tutor = userRepository.findByEmail(tutorEmail).orElse(null);
        List<Test> tutorTests = (tutor != null) ? testRepository.findByCreatedBy(tutor) : Collections.emptyList();
        model.addAttribute("tests", tutorTests);
        model.addAttribute("currentPath", "/tutor/tests");
        return "tutor/tests";
    }

    // 2. Show "Add Test" form
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("currentPath", "/tutor/tests/add");
        return "tutor/add-test";
    }

    // 3. Handle "Add Test" POST
    @PostMapping("/add")
    public String addTest(@RequestParam String title,
                          @RequestParam String description,
                          @RequestParam String type,
                          @RequestParam("pdfFile") MultipartFile pdfFile,
                          Principal principal,
                          RedirectAttributes ra) {
        String tutorEmail = principal.getName();
        User tutor = userRepository.findByEmail(tutorEmail).orElse(null);

        Test test = new Test();
        test.setTitle(title);
        test.setDescription(description);
        test.setType(type);
        test.setCreatedAt(LocalDateTime.now());
        test.setCreatedBy(tutor); // <-- Set the tutor as creator

        try {
            if (!pdfFile.isEmpty()) {
                Path uploads = Paths.get(testSubmissionsDir).toAbsolutePath().normalize();
                Files.createDirectories(uploads);

                String unique = System.currentTimeMillis()
                        + "_" + UUID.randomUUID()
                        + "_" + pdfFile.getOriginalFilename();
                Path dest = uploads.resolve(unique);
                pdfFile.transferTo(dest.toFile());
                test.setPdfPath(unique);
            }
            testRepository.save(test);
            ra.addFlashAttribute("success", "Test created successfully!");
        } catch (IOException e) {
            e.printStackTrace();
            ra.addFlashAttribute("error", "Error uploading PDF: " + e.getMessage());
        }
        return "redirect:/tutor/tests";
    }

    // 4. Show "Edit Test" form
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Optional<Test> opt = testRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Test not found.");
            return "redirect:/tutor/tests";
        }
        model.addAttribute("test", opt.get());
        model.addAttribute("currentPath", "/tutor/tests");
        return "tutor/edit-test";
    }

    // 5. Handle "Edit Test" POST
    @PostMapping("/edit/{id}")
    public String editTest(@PathVariable Long id,
                           @RequestParam String title,
                           @RequestParam String description,
                           @RequestParam String type,
                           RedirectAttributes ra) {
        Optional<Test> opt = testRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Test not found.");
            return "redirect:/tutor/tests";
        }
        Test t = opt.get();
        t.setTitle(title);
        t.setDescription(description);
        t.setType(type);
        testRepository.save(t);
        ra.addFlashAttribute("success", "Test updated successfully.");
        return "redirect:/tutor/tests";
    }

    // 6. Delete a test
    @PostMapping("/delete/{id}")
    public String deleteTest(@PathVariable Long id, RedirectAttributes ra) {
        testRepository.deleteById(id);
        ra.addFlashAttribute("message", "Test deleted.");
        return "redirect:/tutor/tests";
    }

    // 7. Serve PDF inline
    @GetMapping("/pdf/{fileName:.+}")
    public ResponseEntity<Resource> viewTestPdf(@PathVariable String fileName) {
        try {
            Path file = Paths.get(testSubmissionsDir).resolve(fileName).normalize();
            Resource res = new UrlResource(file.toUri());
            if (!res.exists()) return ResponseEntity.notFound().build();

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + res.getFilename() + "\"")
                    .body(res);
        } catch (MalformedURLException e) {
            return ResponseEntity.status(500).build();
        }
    }

    // 8. View all student submissions for tests created by this tutor
    @GetMapping("/submissions")
    public String viewSubmissions(Model model, Principal principal) {
        String tutorEmail = principal.getName();
        User tutor = userRepository.findByEmail(tutorEmail).orElse(null);

        List<Test> tutorTests = (tutor != null) ? testRepository.findByCreatedBy(tutor) : Collections.emptyList();
        List<TestSubmission> filteredSubmissions = new ArrayList<>();

        // Only include submissions where submission.test is in tutorTests
        if (!tutorTests.isEmpty()) {
            List<Long> tutorTestIds = tutorTests.stream().map(Test::getId).collect(Collectors.toList());
            List<TestSubmission> allSubs = submissionRepository.findAll();
            for (TestSubmission sub : allSubs) {
                if (sub.getTest() != null && tutorTestIds.contains(sub.getTest().getId())) {
                    filteredSubmissions.add(sub);
                }
            }
        }

        model.addAttribute("submissions", filteredSubmissions);
        model.addAttribute("currentPath", "/tutor/tests/submissions");
        return "tutor/test-submissions";
    }

    // 9. Download a student-submitted file
    @GetMapping("/submissions/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadSubmission(@PathVariable String fileName) {
        try {
            Path file = Paths.get(testSubmissionsDir).resolve(fileName).normalize();
            Resource res = new UrlResource(file.toUri());
            if (!res.exists()) return ResponseEntity.notFound().build();

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + res.getFilename() + "\"")
                    .body(res);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // 10. Show review form
    @GetMapping("/submissions/review/{id}")
    public String showSubmissionReview(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Optional<TestSubmission> opt = submissionRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Submission not found.");
            return "redirect:/tutor/tests/submissions";
        }
        model.addAttribute("submission", opt.get());
        model.addAttribute("currentPath", "/tutor/tests/submissions");
        return "tutor/review-submission";
    }

    // 11. Handle review POST
    @PostMapping("/submissions/review/{id}")
    public String submitSubmissionReview(@PathVariable Long id,
                                         @RequestParam int marks,
                                         @RequestParam String feedback,
                                         RedirectAttributes ra) {
        Optional<TestSubmission> opt = submissionRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Submission not found.");
            return "redirect:/tutor/tests/submissions";
        }
        TestSubmission sub = opt.get();
        sub.setMarks(marks);
        sub.setFeedback(feedback);
        submissionRepository.save(sub);
        ra.addFlashAttribute("success", "Review saved.");
        return "redirect:/tutor/tests/submissions/review/" + id;
    }

    // 12. Tutor reply to student feedback
    @PostMapping("/submissions/reply/{id}")
    public String replyToStudentReview(@PathVariable Long id,
                                       @RequestParam("reply") String reply,
                                       RedirectAttributes ra) {
        Optional<TestSubmission> opt = submissionRepository.findById(id);
        if (opt.isPresent()) {
            TestSubmission sub = opt.get();
            sub.setTutorReply(reply);
            submissionRepository.save(sub);
            ra.addFlashAttribute("success", "Reply sent to student!");
        } else {
            ra.addFlashAttribute("error", "Submission not found.");
        }
        return "redirect:/tutor/tests/submissions";
    }
}
