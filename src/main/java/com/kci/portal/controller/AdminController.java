package com.kci.portal.controller;

import com.kci.portal.model.*;
import com.kci.portal.repository.*;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Optional;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.core.Authentication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired private UserRepository userRepository;
    @Autowired private TrainingModuleRepository trainingModuleRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private StudentResultRepository studentResultRepository;
    @Autowired private StudentQueryRepository studentQueryRepository;
    @Autowired private TestRepository testRepository;
    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Value("${upload.path.assignment}")
    private String assignmentUploadPath;
    @Value("${assignment.upload.dir}")
    private String assignmentUploadDir;

    @Value("${upload.path.tests}")
    private String testUploadPath;

    @GetMapping("/dashboard")
    public String adminDashboard(Model model, Authentication authentication) {
        User admin = userRepository.findByEmail(authentication.getName()).orElse(null);
        model.addAttribute("user", admin);

        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalModules", trainingModuleRepository.count());
        model.addAttribute("totalAssignments", assignmentRepository.count());
        model.addAttribute("pendingAssignments", assignmentRepository.countByStatus("Pending"));
        model.addAttribute("reviewedAssignments", assignmentRepository.countByStatus("Reviewed"));

        return "admin-dashboard"; // make sure this HTML exists in /templates/
    }


    @GetMapping("/users")
    public String showAllUsers(Model model) {
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("currentPath", "/admin/users");
        return "admin-users";
    }

    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "User deleted successfully.");
        return "redirect:/admin/users";
    }

    @GetMapping("/modules")
    public String getModulesPage(Model model) {
        model.addAttribute("modules", trainingModuleRepository.findAll());
        model.addAttribute("module", new TrainingModule());
        model.addAttribute("currentPath", "/admin/modules");
        return "admin-modules";
    }

    @PostMapping("/modules")
    public String saveModuleInline(@ModelAttribute("module") TrainingModule module,
                                   RedirectAttributes redirectAttributes) {
        trainingModuleRepository.save(module);
        redirectAttributes.addFlashAttribute("message", "Module added successfully.");
        return "redirect:/admin/modules";
    }

    @PostMapping("/modules/delete/{id}")
    public String deleteModule(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        trainingModuleRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Module deleted successfully.");
        return "redirect:/admin/modules";
    }

    @GetMapping("/modules/add")
    public String showAddModuleForm(Model model, HttpServletRequest request) {
        model.addAttribute("module", new TrainingModule());
        model.addAttribute("currentPath", request.getRequestURI());
        return "admin-add-module";
    }

    @PostMapping("/modules/add")
    public String saveModuleFromSeparatePage(@ModelAttribute("module") TrainingModule module,
                                             RedirectAttributes redirectAttributes) {
        trainingModuleRepository.save(module);
        redirectAttributes.addFlashAttribute("message", "Module added successfully!");
        return "redirect:/admin/modules";
    }

    @GetMapping("/modules/edit/{id}")
    public String showEditModuleForm(@PathVariable Long id, Model model,
                                     HttpServletRequest request, RedirectAttributes redirectAttributes) {
        TrainingModule module = trainingModuleRepository.findById(id).orElse(null);
        if (module == null) {
            redirectAttributes.addFlashAttribute("error", "Module not found.");
            return "redirect:/admin/modules";
        }
        model.addAttribute("module", module);
        model.addAttribute("currentPath", request.getRequestURI());
        return "admin-edit-module";
    }

    @PostMapping("/modules/edit/{id}")
    public String updateModule(@PathVariable Long id, @ModelAttribute("module") TrainingModule updatedModule,
                               RedirectAttributes redirectAttributes) {
        TrainingModule existing = trainingModuleRepository.findById(id).orElse(null);
        if (existing == null) {
            redirectAttributes.addFlashAttribute("error", "Module not found.");
            return "redirect:/admin/modules";
        }
        existing.setTitle(updatedModule.getTitle());
        existing.setDescription(updatedModule.getDescription());
        trainingModuleRepository.save(existing);
        redirectAttributes.addFlashAttribute("message", "Module updated successfully.");
        return "redirect:/admin/modules";
    }

    @PostMapping("/assignments/delete/{id}")
    public String deleteAssignment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        assignmentRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Assignment deleted successfully.");
        return "redirect:/admin/assignments";
    }

    @GetMapping("/assignments")
    public String viewAssignments(Model model) {
        List<Assignment> assignments = assignmentRepository.findAllWithUser();
        model.addAttribute("assignments", assignments);
        return "admin-view-assignments";
    }

    @GetMapping("/assignments/review/{id}")
    public String showReviewForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Assignment assignment = assignmentRepository.findById(id).orElse(null);
        if (assignment == null) {
            redirectAttributes.addFlashAttribute("error", "Assignment not found.");
            return "redirect:/admin/assignments";
        }
        model.addAttribute("assignment", assignment);
        return "admin-review-assignment";
    }

    @PostMapping("/assignments/review/{id}")
    public String submitReview(@PathVariable Long id,
                               @RequestParam("solutionFile") MultipartFile solutionFile,
                               @RequestParam("feedback") String feedback,
                               RedirectAttributes redirectAttributes) {

        Optional<Assignment> optionalAssignment = assignmentRepository.findById(id);
        if (optionalAssignment.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Assignment not found.");
            return "redirect:/admin/assignments";
        }

        Assignment assignment = optionalAssignment.get();

        try {
            // Resolve & create the directory if it doesn't exist
            Path uploadsDir = Paths.get(assignmentUploadPath)
                    .toAbsolutePath()
                    .normalize();
            Files.createDirectories(uploadsDir);

            if (solutionFile != null && !solutionFile.isEmpty()) {
                // Clean the original filename
                String originalFilename = StringUtils.cleanPath(solutionFile.getOriginalFilename());
                // Prepend timestamp to avoid collisions
                String fileName = System.currentTimeMillis() + "_" + originalFilename;
                Path filePath = uploadsDir.resolve(fileName);

                // Save the file to disk
                solutionFile.transferTo(filePath.toFile());

                // Store the relative filename in the entity
                assignment.setSolutionPath(fileName);
            }

            // Save feedback & status
            assignment.setFeedback(feedback);
            assignment.setStatus("Reviewed");
            assignmentRepository.save(assignment);

            redirectAttributes.addFlashAttribute("message", "Review submitted successfully.");
        } catch (IOException ex) {
            ex.printStackTrace();
            redirectAttributes.addFlashAttribute("error",
                    "Error uploading solution file: " + ex.getMessage());
        }

        // Redirect back to the review page
        return "redirect:/admin/assignments/review/" + id;
    }
    @GetMapping("/results/upload")
    public String showUploadResultForm(Model model) {
        model.addAttribute("students", userRepository.findAll().stream()
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

        StudentResult result = new StudentResult();
        result.setUser(student);
        result.setModuleTitle(moduleTitle);
        result.setTestName(testName);
        result.setScore(score);
        result.setPassed(passed);
        result.setDateTaken(LocalDate.parse(dateTaken));

        studentResultRepository.save(result);
        return "redirect:/admin/results/upload?success";
    }

    @GetMapping("/results")
    public String viewAllResults(Model model) {
        List<StudentResult> results = studentResultRepository.findAll();
        model.addAttribute("results", results);
        model.addAttribute("currentPath", "/admin/results");
        return "admin-view-results";
    }

    @GetMapping("/results/export/pdf")
    public void exportResultsAsPdf(HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=student-results.pdf");

        List<StudentResult> results = studentResultRepository.findAll();

        Document document = new Document();
        try {
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph title = new Paragraph("Student Results Report", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new int[]{1, 3, 3, 3, 3, 2, 2});

            Stream.of("ID", "Name", "Email", "Module", "Test", "Score", "Status")
                    .forEach(header -> {
                        PdfPCell cell = new PdfPCell();
                        cell.setPhrase(new Phrase(header));
                        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                        table.addCell(cell);
                    });

            for (StudentResult result : results) {
                table.addCell(String.valueOf(result.getId()));
                table.addCell(result.getUser().getFullName());
                table.addCell(result.getUser().getEmail());
                table.addCell(result.getModuleTitle());
                table.addCell(result.getTestName());
                table.addCell(String.valueOf(result.getScore()));
                table.addCell(result.isPassed() ? "Passed" : "Failed");
            }

            document.add(table);
        } catch (DocumentException e) {
            throw new IOException("Error generating PDF", e);
        } finally {
            document.close();
        }
    }

    @GetMapping("/queries")
    public String viewStudentQueries(Model model) {
        List<StudentQuery> queries = studentQueryRepository.findAll();
        model.addAttribute("queries", queries);
        model.addAttribute("currentPath", "/admin/queries");
        return "admin-view-queries";
    }

    @PostMapping("/queries/reply/{id}")
    public String replyToQuery(@PathVariable Long id,
                               @RequestParam("reply") String reply,
                               RedirectAttributes redirectAttributes) {
        StudentQuery query = studentQueryRepository.findById(id).orElse(null);
        if (query != null) {
            query.setReply(reply);
            query.setAnswer(reply);
            query.setStatus("Answered");
            query.setRepliedAt(java.time.LocalDateTime.now());
            studentQueryRepository.save(query);
            redirectAttributes.addFlashAttribute("message", "Reply sent successfully.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Query not found.");
        }
        return "redirect:/admin/queries";
    }

    @GetMapping("/tests")
    public String showTestManagementPage(Model model) {
        List<Test> tests = testRepository.findAll();
        model.addAttribute("tests", tests);
        model.addAttribute("currentPath", "/admin/tests");
        return "admin-tests";
    }

    @GetMapping("/tests/add")
    public String showAddTestForm(Model model) {
        return "admin-add-test";
    }
    @PostMapping("/tests/add")
    public String saveTestFromAddPage(@RequestParam String title,
                                      @RequestParam String description,
                                      @RequestParam String type,
                                      @RequestParam("pdfFile") MultipartFile pdfFile,
                                      RedirectAttributes redirectAttributes) {

        Test test = new Test();
        test.setTitle(title);
        test.setDescription(description);
        test.setType(type);

        try {
            if (!pdfFile.isEmpty()) {
                Path uploadsDir = Paths.get(testUploadPath);
                Files.createDirectories(uploadsDir);
                String fileName = System.currentTimeMillis() + "_" + pdfFile.getOriginalFilename();
                Path path = uploadsDir.resolve(fileName);
                pdfFile.transferTo(path.toFile());
                test.setPdfPath(fileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error uploading PDF.");
            return "redirect:/admin/tests";
        }

        testRepository.save(test);
        redirectAttributes.addFlashAttribute("message", "Test created successfully!");
        return "redirect:/admin/tests";
    }
    @PostMapping("/tests")
    public String saveTest(@RequestParam String title,
                           @RequestParam String description,
                           @RequestParam String type,
                           @RequestParam("pdfFile") MultipartFile pdfFile,
                           HttpServletRequest request,
                           RedirectAttributes redirectAttributes) {

        Test test = new Test();
        test.setTitle(title);
        test.setDescription(description);
        test.setType(type);

        try {
            if (!pdfFile.isEmpty()) {
                String uploadsDir = request.getServletContext().getRealPath("/test-pdfs/");
                Files.createDirectories(Paths.get(uploadsDir));
                String fileName = System.currentTimeMillis() + "_" + pdfFile.getOriginalFilename();
                Path path = Paths.get(uploadsDir, fileName);
                pdfFile.transferTo(path.toFile());
                test.setPdfPath(fileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error uploading PDF.");
            return "redirect:/admin/tests";
        }

        testRepository.save(test);
        redirectAttributes.addFlashAttribute("message", "Test created successfully!");
        return "redirect:/admin/tests";
    }

    @PostMapping("/tests/delete/{id}")
    public String deleteTest(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        testRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Test deleted successfully.");
        return "redirect:/admin/tests";
    }

    @GetMapping("/tests/edit/{id}")
    public String showEditTestForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Test test = testRepository.findById(id).orElse(null);
        if (test == null) {
            redirectAttributes.addFlashAttribute("error", "Test not found.");
            return "redirect:/admin/tests";
        }
        model.addAttribute("test", test);
        return "admin-edit-test";
    }

    @PostMapping("/tests/edit/{id}")
    public String updateTest(@PathVariable Long id,
                             @RequestParam String title,
                             @RequestParam String description,
                             @RequestParam String type,
                             RedirectAttributes redirectAttributes) {
        Test test = testRepository.findById(id).orElse(null);
        if (test == null) {
            redirectAttributes.addFlashAttribute("error", "Test not found.");
            return "redirect:/admin/tests";
        }

        test.setTitle(title);
        test.setDescription(description);
        test.setType(type);
        testRepository.save(test);

        redirectAttributes.addFlashAttribute("success", "Test updated successfully!");
        return "redirect:/admin/tests/edit/" + id;
    }
    @Autowired
    private TestSubmissionRepository testSubmissionRepository;

    @GetMapping("/review-results")
    public String reviewStudentSubmissions(Model model) {
        List<TestSubmission> submissions = testSubmissionRepository.findAll();
        model.addAttribute("submissions", submissions);
        return "admin-review-results";
    }

    @PostMapping("/reply-result")
    public String replyToStudentResult(
            @RequestParam Long submissionId,
            @RequestParam int marks,
            @RequestParam String feedback) {

        Optional<TestSubmission> optional = testSubmissionRepository.findById(submissionId);
        if (optional.isPresent()) {
            TestSubmission submission = optional.get();
            submission.setMarks(marks);
            submission.setFeedback(feedback);
            testSubmissionRepository.save(submission);
        }

        return "redirect:/admin/review-results";
    }
    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            // Update path to test_submissions
            Path filePath = Paths.get("uploads/test_submissions").resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                System.out.println("File not found: " + filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    @PostMapping("/admin/reply-result")
    public String replyToResult(@RequestParam Long submissionId,
                                @RequestParam Integer marks,
                                @RequestParam String feedback,
                                RedirectAttributes redirectAttributes) {

        Optional<TestSubmission> submissionOpt = testSubmissionRepository.findById(submissionId);
        if (submissionOpt.isPresent()) {
            TestSubmission submission = submissionOpt.get();
            submission.setMarks(marks);
            submission.setFeedback(feedback);
            testSubmissionRepository.save(submission);

            redirectAttributes.addFlashAttribute("success", "Reply submitted successfully.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Submission not found.");
        }

        return "redirect:/admin/review-results";
    }
    @GetMapping("/chat/active-threads")
    @PreAuthorize("hasRole('ADMIN')")
    public String viewAllActiveChatUsers(Model model) {
        List<User> students = chatMessageRepository.findStudentsInvolvedInChat();
        List<User> tutors = chatMessageRepository.findTutorsInvolvedInChat();

        model.addAttribute("students", students);
        model.addAttribute("tutors", tutors);

        return "admin/admin-active-chat-threads";
    }
    @GetMapping("/tests/pdf/{fileName:.+}")
    public ResponseEntity<Resource> viewTestPdf(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get(testUploadPath).resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
