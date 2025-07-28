package com.kci.portal.controller;

import com.kci.portal.model.*;
import com.kci.portal.repository.*;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired private UserRepository              userRepository;
    @Autowired private TrainingModuleRepository    trainingModuleRepository;
    @Autowired private AssignmentRepository        assignmentRepository;
    @Autowired private StudentResultRepository     studentResultRepository;
    @Autowired private StudentQueryRepository      studentQueryRepository;
    @Autowired private TestRepository              testRepository;
    @Autowired private TestSubmissionRepository    testSubmissionRepository;
    @Autowired private ChatMessageRepository       chatMessageRepository;

    @Value("${upload.path.assignment}")
    private String assignmentUploadPath;

    // default to uploads/test_submissions if no property set
    @Value("${upload.path.test_submissions:uploads/test_submissions}")
    private String testSubmissionsDir;

    //
    // ─── DASHBOARD ───────────────────────────────────────────────────────────────────────
    //

    @GetMapping("/dashboard")
    public String adminDashboard(Model model, Authentication authentication) {
        User admin = userRepository.findByEmail(authentication.getName()).orElse(null);
        model.addAttribute("user", admin);
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalModules", trainingModuleRepository.count());
        model.addAttribute("totalAssignments", assignmentRepository.count());
        model.addAttribute("pendingAssignments", assignmentRepository.countByStatus("Pending"));
        model.addAttribute("reviewedAssignments", assignmentRepository.countByStatus("Reviewed"));
        return "admin-dashboard";
    }

    //
    // ─── USER MANAGEMENT ────────────────────────────────────────────────────────────────
    //

    @GetMapping("/users")
    public String showAllUsers(Model model) {
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("currentPath", "/admin/users");
        return "admin-users";
    }

    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes ra) {
        userRepository.deleteById(id);
        ra.addFlashAttribute("message", "User deleted successfully.");
        return "redirect:/admin/users";
    }

    //
    // ─── MODULE MANAGEMENT ──────────────────────────────────────────────────────────────
    //

    @GetMapping("/modules")
    public String getModulesPage(Model model) {
        model.addAttribute("modules", trainingModuleRepository.findAll());
        model.addAttribute("module", new TrainingModule());
        model.addAttribute("currentPath", "/admin/modules");
        return "admin-modules";
    }

    @PostMapping("/modules")
    public String saveModuleInline(@ModelAttribute TrainingModule module,
                                   RedirectAttributes ra) {
        trainingModuleRepository.save(module);
        ra.addFlashAttribute("message", "Module added successfully.");
        return "redirect:/admin/modules";
    }

    @PostMapping("/modules/delete/{id}")
    public String deleteModule(@PathVariable Long id, RedirectAttributes ra) {
        trainingModuleRepository.deleteById(id);
        ra.addFlashAttribute("message", "Module deleted successfully.");
        return "redirect:/admin/modules";
    }

    @GetMapping("/modules/add")
    public String showAddModuleForm(Model model) {
        model.addAttribute("module", new TrainingModule());
        model.addAttribute("currentPath", "/admin/modules/add");
        return "admin-add-module";
    }

    @PostMapping("/modules/add")
    public String saveModuleFromSeparatePage(@ModelAttribute TrainingModule module,
                                             RedirectAttributes ra) {
        trainingModuleRepository.save(module);
        ra.addFlashAttribute("message", "Module added successfully!");
        return "redirect:/admin/modules";
    }

    @GetMapping("/modules/edit/{id}")
    public String showEditModuleForm(@PathVariable Long id,
                                     Model model,
                                     RedirectAttributes ra) {
        TrainingModule module = trainingModuleRepository.findById(id).orElse(null);
        if (module == null) {
            ra.addFlashAttribute("error", "Module not found.");
            return "redirect:/admin/modules";
        }
        model.addAttribute("module", module);
        model.addAttribute("currentPath", "/admin/modules/edit/" + id);
        return "admin-edit-module";
    }

    @PostMapping("/modules/edit/{id}")
    public String updateModule(@PathVariable Long id,
                               @ModelAttribute TrainingModule updatedModule,
                               RedirectAttributes ra) {
        TrainingModule existing = trainingModuleRepository.findById(id).orElse(null);
        if (existing == null) {
            ra.addFlashAttribute("error", "Module not found.");
            return "redirect:/admin/modules";
        }
        existing.setTitle(updatedModule.getTitle());
        existing.setDescription(updatedModule.getDescription());
        trainingModuleRepository.save(existing);
        ra.addFlashAttribute("message", "Module updated successfully.");
        return "redirect:/admin/modules";
    }

    //
    // ─── ASSIGNMENT MANAGEMENT ──────────────────────────────────────────────────────────
    //

    @GetMapping("/assignments")
    public String viewAssignments(Model model) {
        model.addAttribute("assignments", assignmentRepository.findAllWithUser());
        return "admin-view-assignments";
    }

    @PostMapping("/assignments/delete/{id}")
    public String deleteAssignment(@PathVariable Long id, RedirectAttributes ra) {
        assignmentRepository.deleteById(id);
        ra.addFlashAttribute("message", "Assignment deleted successfully.");
        return "redirect:/admin/assignments";
    }

    @GetMapping("/assignments/review/{id}")
    public String showReviewForm(@PathVariable Long id,
                                 Model model,
                                 RedirectAttributes ra) {
        Assignment a = assignmentRepository.findById(id).orElse(null);
        if (a == null) {
            ra.addFlashAttribute("error", "Assignment not found.");
            return "redirect:/admin/assignments";
        }
        model.addAttribute("assignment", a);
        return "admin-review-assignment";
    }

    @PostMapping("/assignments/review/{id}")
    public String submitReview(@PathVariable Long id,
                               @RequestParam("solutionFile") MultipartFile solutionFile,
                               @RequestParam("feedback") String feedback,
                               RedirectAttributes ra) {
        Optional<Assignment> opt = assignmentRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Assignment not found.");
            return "redirect:/admin/assignments";
        }
        Assignment a = opt.get();
        try {
            Path uploads = Paths.get(assignmentUploadPath).toAbsolutePath().normalize();
            Files.createDirectories(uploads);

            if (!solutionFile.isEmpty()) {
                String orig = StringUtils.cleanPath(solutionFile.getOriginalFilename());
                String fileName = System.currentTimeMillis() + "_" + orig;
                Path dest = uploads.resolve(fileName);
                solutionFile.transferTo(dest.toFile());
                a.setSolutionPath(fileName);
            }

            a.setFeedback(feedback);
            a.setStatus("Reviewed");
            assignmentRepository.save(a);
            ra.addFlashAttribute("message", "Review submitted successfully.");
        } catch (IOException e) {
            e.printStackTrace();
            ra.addFlashAttribute("error", "Error uploading solution file: " + e.getMessage());
        }
        return "redirect:/admin/assignments/review/" + id;
    }

    //
    // ─── STUDENT RESULTS ───────────────────────────────────────────────────────────────
    //

    @GetMapping("/results/upload")
    public String showUploadResultForm(Model model) {
        model.addAttribute("students",
                userRepository.findAll().stream()
                        .filter(u -> u.getRoles().contains("ROLE_STUDENT"))
                        .toList());
        model.addAttribute("currentPath", "/admin/results/upload");
        return "admin-upload-result";
    }

    @PostMapping("/results/upload")
    public String handleResultUpload(@RequestParam Long userId,
                                     @RequestParam String moduleTitle,
                                     @RequestParam String testName,
                                     @RequestParam int score,
                                     @RequestParam boolean passed,
                                     @RequestParam String dateTaken) {
        User student = userRepository.findById(userId).orElse(null);
        if (student == null) {
            return "redirect:/admin/results/upload?error";
        }
        StudentResult r = new StudentResult();
        r.setUser(student);
        r.setModuleTitle(moduleTitle);
        r.setTestName(testName);
        r.setScore(score);
        r.setPassed(passed);
        r.setDateTaken(LocalDate.parse(dateTaken));
        studentResultRepository.save(r);
        return "redirect:/admin/results/upload?success";
    }

    @GetMapping("/results")
    public String viewAllResults(Model model) {
        model.addAttribute("results", studentResultRepository.findAll());
        model.addAttribute("currentPath", "/admin/results");
        return "admin-view-results";
    }

    @GetMapping("/results/export/pdf")
    public void exportResultsAsPdf(HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition","attachment; filename=student-results.pdf");
        List<StudentResult> results = studentResultRepository.findAll();
        Document doc = new Document();
        try {
            PdfWriter.getInstance(doc, response.getOutputStream());
            doc.open();
            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD,16);
            Paragraph title = new Paragraph("Student Results Report", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);
            doc.add(new Paragraph(" "));
            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new int[]{1,3,3,3,3,2,2});
            Stream.of("ID","Name","Email","Module","Test","Score","Status")
                    .forEach(h -> {
                        PdfPCell c = new PdfPCell();
                        c.setPhrase(new Phrase(h));
                        c.setBackgroundColor(BaseColor.LIGHT_GRAY);
                        table.addCell(c);
                    });
            for (StudentResult r : results) {
                table.addCell(String.valueOf(r.getId()));
                table.addCell(r.getUser().getFullName());
                table.addCell(r.getUser().getEmail());
                table.addCell(r.getModuleTitle());
                table.addCell(r.getTestName());
                table.addCell(String.valueOf(r.getScore()));
                table.addCell(r.isPassed() ? "Passed" : "Failed");
            }
            doc.add(table);
        } catch (DocumentException e) {
            throw new IOException("Error generating PDF", e);
        } finally {
            doc.close();
        }
    }

    //
    // ─── STUDENT QUERIES ───────────────────────────────────────────────────────────────
    //

    @GetMapping("/queries")
    public String viewStudentQueries(Model model) {
        model.addAttribute("queries", studentQueryRepository.findAll());
        model.addAttribute("currentPath", "/admin/queries");
        return "admin-view-queries";
    }

    @PostMapping("/queries/reply/{id}")
    public String replyToQuery(@PathVariable Long id,
                               @RequestParam("reply") String reply,
                               RedirectAttributes ra) {
        Optional<StudentQuery> oq = studentQueryRepository.findById(id);
        if (oq.isPresent()) {
            StudentQuery q = oq.get();
            q.setReply(reply);
            q.setAnswer(reply);
            q.setStatus("Answered");
            q.setRepliedAt(LocalDateTime.now());
            studentQueryRepository.save(q);
            ra.addFlashAttribute("message","Reply sent successfully.");
        } else {
            ra.addFlashAttribute("error","Query not found.");
        }
        return "redirect:/admin/queries";
    }

    //
    // ─── TEST MANAGEMENT ──────────────────────────────────────────────────────────────
    //

    @GetMapping("/tests")
    public String showTestManagementPage(Model model) {
        model.addAttribute("tests", testRepository.findAll());
        model.addAttribute("currentPath","/admin/tests");
        return "admin-tests";
    }

    @GetMapping("/tests/add")
    public String showAddTestForm(Model model) {
        model.addAttribute("currentPath","/admin/tests/add");
        return "admin-add-test";
    }

    @PostMapping("/tests/add")
    public String saveTestFromAddPage(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam String type,
            @RequestParam("pdfFile") MultipartFile pdfFile,
            RedirectAttributes ra
    ) {
        Test test = new Test();
        test.setTitle(title);
        test.setDescription(description);
        test.setType(type);
        test.setCreatedAt(LocalDateTime.now());

        try {
            if (!pdfFile.isEmpty()) {
                Path uploads = Paths.get(testSubmissionsDir).toAbsolutePath().normalize();
                Files.createDirectories(uploads);

                String orig = StringUtils.cleanPath(pdfFile.getOriginalFilename());
                String unique = System.currentTimeMillis()
                        + "_" + UUID.randomUUID()
                        + "_" + orig;

                Path dest = uploads.resolve(unique);
                pdfFile.transferTo(dest.toFile());
                test.setPdfPath(unique);
            }
        } catch (IOException e) {
            e.printStackTrace();
            ra.addFlashAttribute("error","Error uploading PDF: " + e.getMessage());
            return "redirect:/admin/tests/add";
        }

        testRepository.save(test);
        ra.addFlashAttribute("message","Test created successfully!");
        return "redirect:/admin/tests";
    }

    @PostMapping("/tests/delete/{id}")
    public String deleteTest(@PathVariable Long id, RedirectAttributes ra) {
        testRepository.deleteById(id);
        ra.addFlashAttribute("message","Test deleted successfully.");
        return "redirect:/admin/tests";
    }

    @GetMapping("/tests/edit/{id}")
    public String showEditTestForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Test t = testRepository.findById(id).orElse(null);
        if (t == null) {
            ra.addFlashAttribute("error","Test not found.");
            return "redirect:/admin/tests";
        }
        model.addAttribute("test", t);
        model.addAttribute("currentPath","/admin/tests/edit/" + id);
        return "admin-edit-test";
    }

    @PostMapping("/tests/edit/{id}")
    public String updateTest(@PathVariable Long id,
                             @RequestParam String title,
                             @RequestParam String description,
                             @RequestParam String type,
                             RedirectAttributes ra) {
        Test t = testRepository.findById(id).orElse(null);
        if (t == null) {
            ra.addFlashAttribute("error","Test not found.");
            return "redirect:/admin/tests";
        }
        t.setTitle(title);
        t.setDescription(description);
        t.setType(type);
        testRepository.save(t);
        ra.addFlashAttribute("message","Test updated successfully!");
        return "redirect:/admin/tests";
    }

    /** Serve uploaded test PDF inline */
    @GetMapping("/tests/pdf/{fileName:.+}")
    public ResponseEntity<Resource> viewTestPdf(@PathVariable String fileName) {
        try {
            Path file = Paths.get(testSubmissionsDir).resolve(fileName).normalize();
            Resource res = new UrlResource(file.toUri());
            if (!res.exists()) return ResponseEntity.notFound().build();

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + res.getFilename() + "\"")
                    .body(res);
        } catch (MalformedURLException e) {
            return ResponseEntity.status(500).build();
        }
    }

    //
    // ─── REVIEW STUDENT TEST SUBMISSIONS ─────────────────────────────────────────────────
    //

    @GetMapping("/review-results")
    public String reviewStudentSubmissions(Model model) {
        model.addAttribute("submissions", testSubmissionRepository.findAll());
        return "admin/review-results";
    }

    @PostMapping("/reply-result")
    public String replyToStudentResult(@RequestParam Long submissionId,
                                       @RequestParam int marks,
                                       @RequestParam String feedback) {
        Optional<TestSubmission> op = testSubmissionRepository.findById(submissionId);
        if (op.isPresent()) {
            TestSubmission s = op.get();
            s.setMarks(marks);
            s.setFeedback(feedback);
            testSubmissionRepository.save(s);
        }
        return "redirect:/admin/review-results";
    }

    /** Download a student‐submitted file */
    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            Path file = Paths.get(testSubmissionsDir).resolve(fileName).normalize();
            Resource res = new UrlResource(file.toUri());
            if (!res.exists()) return ResponseEntity.notFound().build();

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + res.getFilename() + "\"")
                    .body(res);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    //
    // ─── ACTIVE CHAT THREADS ───────────────────────────────────────────────────────────
    //

    @GetMapping("/chat/active-threads")
    public String viewAllActiveChatUsers(Model model) {
        model.addAttribute("students", chatMessageRepository.findStudentsInvolvedInChat());
        model.addAttribute("tutors",   chatMessageRepository.findTutorsInvolvedInChat());
        return "admin/admin-active-chat-threads";
    }
    // in AdminController
    @GetMapping("/tests/tutor-reviews")
    public String viewTutorReviews(Model model) {
        model.addAttribute("reviews",
                testSubmissionRepository.findByMarksIsNotNull());
        model.addAttribute("currentPath", "/admin/tests/tutor-reviews");
        return "admin/tutor-reviews";
    }
    // in AdminController
    @GetMapping("/tests/student-feedback")
    public String viewStudentFeedback(Model model) {
        model.addAttribute("feedbacks",
                testSubmissionRepository.findByStudentFeedbackIsNotNull());
        model.addAttribute("currentPath", "/admin/tests/student-feedback");
        return "admin/admin-student-feedback";
    }
    @GetMapping("/tests/tutor-reviews/view/{id}")
    public String viewTutorReviewDetail(@PathVariable Long id, Model model) {
        Optional<TestSubmission> submission = testSubmissionRepository.findById(id);
        if (submission.isPresent()) {
            model.addAttribute("submission", submission.get());
        } else {
            model.addAttribute("error", "Submission not found.");
            model.addAttribute("submission", null);
        }
        model.addAttribute("currentPath", "/admin/tests/tutor-reviews");
        return "admin/tutor-review-detail";
    }
    @PostMapping("/tests/tutor-reviews/review/{id}")
    public String reviewTutorSubmission(
            @PathVariable Long id,
            @RequestParam int marks,
            @RequestParam String feedback,
            RedirectAttributes ra
    ) {
        Optional<TestSubmission> opt = testSubmissionRepository.findById(id);
        if (opt.isPresent()) {
            TestSubmission sub = opt.get();
            sub.setMarks(marks);
            sub.setFeedback(feedback);
            sub.setReviewedAt(LocalDateTime.now());
            testSubmissionRepository.save(sub);
            ra.addFlashAttribute("success", "Review submitted successfully!");
        } else {
            ra.addFlashAttribute("error", "Submission not found.");
        }
        return "redirect:/admin/tests/tutor-reviews/view/" + id;
    }
    // Show edit form
    @GetMapping("/tests/tutor-reviews/edit/{id}")
    public String showEditTutorReview(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Optional<TestSubmission> opt = testSubmissionRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Submission not found.");
            return "redirect:/admin/tests/tutor-reviews";
        }
        model.addAttribute("submission", opt.get());
        model.addAttribute("currentPath", "/admin/tests/tutor-reviews");
        return "admin/tutor-review-edit";
    }

    // Handle edit POST
    @PostMapping("/tests/tutor-reviews/edit/{id}")
    public String submitEditTutorReview(@PathVariable Long id,
                                        @RequestParam int marks,
                                        @RequestParam String feedback,
                                        Authentication auth,
                                        RedirectAttributes ra) {
        Optional<TestSubmission> opt = testSubmissionRepository.findById(id);
        if (opt.isPresent()) {
            TestSubmission sub = opt.get();
            sub.setMarks(marks);
            sub.setFeedback(feedback);
            sub.setReviewedAt(LocalDateTime.now());
            // New lines:
            sub.setLastReviewedBy(auth.getName());
            sub.setLastReviewedAt(LocalDateTime.now());
            testSubmissionRepository.save(sub);
            ra.addFlashAttribute("success", "Review updated successfully!");
            return "redirect:/admin/tests/tutor-reviews/view/" + id;
        } else {
            ra.addFlashAttribute("error", "Submission not found.");
            return "redirect:/admin/tests/tutor-reviews";
        }
    }
    @PostMapping("/tests/tutor-reviews/reply/{id}")
    public String replyToStudentReview(@PathVariable Long id,
                                       @RequestParam String reply,
                                       RedirectAttributes ra) {
        Optional<TestSubmission> opt = testSubmissionRepository.findById(id);
        if (opt.isPresent()) {
            TestSubmission sub = opt.get();
            sub.setAdminReply(reply);
            testSubmissionRepository.save(sub);
            ra.addFlashAttribute("success", "Reply sent to student!");
        } else {
            ra.addFlashAttribute("error", "Submission not found.");
        }
        return "redirect:/admin/tests/tutor-reviews/view/" + id;
    }
}
