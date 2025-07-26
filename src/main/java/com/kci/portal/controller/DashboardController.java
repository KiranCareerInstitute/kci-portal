package com.kci.portal.controller;

import com.kci.portal.model.Assignment;
import com.kci.portal.model.StudentQuery;
import com.kci.portal.model.User;
import com.kci.portal.repository.AssignmentRepository;
import com.kci.portal.repository.StudentQueryRepository;
import com.kci.portal.repository.TrainingModuleRepository;
import com.kci.portal.repository.UserRepository;
import com.kci.portal.service.StudentResultService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static com.kci.portal.constants.FileConstants.UPLOAD_DIR;
import org.springframework.beans.factory.annotation.Value;

@Controller
public class DashboardController {

    @Autowired private UserRepository userRepository;
    @Autowired private TrainingModuleRepository trainingModuleRepository;
    @Autowired private StudentResultService studentResultService;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private StudentQueryRepository studentQueryRepository;
    @Autowired

    @Value("${upload.path.assignment}")
    private String assignmentUploadPath;

    private void addLoggedInUserToModel(Model model, Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            User user = getCurrentUser(auth);
            model.addAttribute("user", user);
        }
    }

    private User getCurrentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return "redirect:/login";

        User user = getCurrentUser(auth);
        if (user == null) return "redirect:/login";

        model.addAttribute("user", user);
        model.addAttribute("currentPath", "/dashboard");

        if (user.hasRole("ROLE_ADMIN")) {
            return "redirect:/admin";
        } else if (user.hasRole("ROLE_TUTOR")) {
            return "tutor-dashboard";
        } else if (user.hasRole("ROLE_STUDENT") || user.hasRole("ROLE_USER")) {
            return "dashboard"; // ✅ resolves 404/500 for ROLE_USER
        } else {
            return "access-denied";
        }
    }

    @GetMapping("/courses")
    public String courses(Model model, Authentication auth) {
        addLoggedInUserToModel(model, auth);
        model.addAttribute("modules", trainingModuleRepository.findAll());
        model.addAttribute("currentPath", "/courses");
        return "courses";
    }

    @GetMapping("/results")
    public String results(Model model, Authentication auth) {
        addLoggedInUserToModel(model, auth);
        User user = getCurrentUser(auth);
        model.addAttribute("results", studentResultService.getResultsByUser(user));
        model.addAttribute("currentPath", "/results");
        return "results";
    }

    @GetMapping("/results/download-pdf")
    public void downloadMyResultsAsPdf(HttpServletResponse response, Authentication auth) throws IOException {
        if (auth == null || !auth.isAuthenticated()) {
            response.sendRedirect("/login");
            return;
        }

        User user = getCurrentUser(auth);
        List<com.kci.portal.model.StudentResult> results = studentResultService.getResultsByUser(user);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=my-results.pdf");

        com.itextpdf.text.Document document = new com.itextpdf.text.Document();
        try {
            com.itextpdf.text.pdf.PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            com.itextpdf.text.Font fontTitle = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 16);
            com.itextpdf.text.Paragraph title = new com.itextpdf.text.Paragraph("My Results Report", fontTitle);
            title.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            document.add(title);
            document.add(new com.itextpdf.text.Paragraph(" "));

            com.itextpdf.text.pdf.PdfPTable table = new com.itextpdf.text.pdf.PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new int[]{3, 3, 2, 2, 2});

            Stream.of("Module", "Test", "Score", "Status", "Date").forEach(header -> {
                com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell();
                cell.setPhrase(new com.itextpdf.text.Phrase(header));
                cell.setBackgroundColor(com.itextpdf.text.BaseColor.LIGHT_GRAY);
                table.addCell(cell);
            });

            for (com.kci.portal.model.StudentResult result : results) {
                table.addCell(result.getModuleTitle());
                table.addCell(result.getTestName());
                table.addCell(result.getScore() + "%");
                table.addCell(result.isPassed() ? "Passed" : "Failed");
                table.addCell(result.getDateTaken().toString());
            }

            document.add(table);
        } catch (com.itextpdf.text.DocumentException e) {
            throw new IOException("Error generating PDF", e);
        } finally {
            document.close();
        }
    }

    @GetMapping("/profile")
    public String profile(Model model, Authentication auth) {
        addLoggedInUserToModel(model, auth);
        model.addAttribute("currentPath", "/profile");
        return "profile";
    }

    @GetMapping("/profile/edit")
    public String editProfileForm(Model model, Authentication auth) {
        addLoggedInUserToModel(model, auth);
        model.addAttribute("currentPath", "/profile");
        return "profile-edit";
    }

    @PostMapping("/profile/edit")
    public String updateProfile(@ModelAttribute("user") User updatedUser, Authentication auth) {
        User user = getCurrentUser(auth);
        if (user != null) {
            user.setFullName(updatedUser.getFullName());
            user.setMobile(updatedUser.getMobile());
            userRepository.save(user);
        }
        return "redirect:/profile";
    }

    @GetMapping("/profile/change-password")
    public String changePasswordForm(Model model, Authentication auth) {
        addLoggedInUserToModel(model, auth);
        model.addAttribute("currentPath", "/profile");
        return "change-password";
    }

    @PostMapping("/profile/change-password")
    public String updatePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Authentication auth,
                                 Model model) {
        User user = getCurrentUser(auth);
        if (user == null) return "redirect:/login";

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        if (!encoder.matches(currentPassword, user.getPassword())) {
            model.addAttribute("error", "Current password is incorrect");
            return "change-password";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "New passwords do not match");
            return "change-password";
        }

        user.setPassword(encoder.encode(newPassword));
        userRepository.save(user);
        model.addAttribute("success", "Password updated successfully");
        return "change-password";
    }

    @GetMapping("/student/assignments")
    public String viewMyAssignments(Model model, Authentication auth) {
        addLoggedInUserToModel(model, auth);
        User user = getCurrentUser(auth);
        model.addAttribute("assignments", assignmentRepository.findByUser(user));
        model.addAttribute("currentPath", "/student/assignments");
        return "student-view-assignments";
    }

    @GetMapping("/student/assignments/upload")
    public String showAssignmentUploadForm(Model model, Authentication auth) {
        addLoggedInUserToModel(model, auth);
        model.addAttribute("assignment", new Assignment());
        model.addAttribute("currentPath", "/student/assignments/upload");
        return "student-upload-assignment";
    }

    @PostMapping("/student/assignments/upload")
    public String submitAssignment(@ModelAttribute Assignment assignment,
                                   @RequestParam MultipartFile file,
                                   Authentication auth,
                                   RedirectAttributes redirectAttributes) {
        if (auth == null || !auth.isAuthenticated()) return "redirect:/login";

        try {
            User user = getCurrentUser(auth);

            if (!file.isEmpty()) {
                File uploadPath = new File(assignmentUploadPath);
                if (!uploadPath.exists()) uploadPath.mkdirs();

                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                file.transferTo(new File(uploadPath, fileName));
                assignment.setFilePath(fileName);
            }

            assignment.setUser(user);
            assignmentRepository.save(assignment);
            redirectAttributes.addFlashAttribute("message", "Assignment uploaded successfully!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("message", "Failed to upload assignment.");
            e.printStackTrace();
        }

        return "redirect:/student/assignments/upload";
    }

    @GetMapping("/student/assignment/feedback/{id}")
    public String viewAssignmentFeedback(@PathVariable Long id, Model model, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return "redirect:/login";

        Assignment assignment = assignmentRepository.findById(id).orElse(null);
        User user = getCurrentUser(auth);

        if (assignment == null || !assignment.getUser().getId().equals(user.getId())) {
            return "redirect:/student/assignments";
        }

        model.addAttribute("assignment", assignment);
        model.addAttribute("currentPath", "/student/assignments");
        addLoggedInUserToModel(model, auth);
        return "student-view-feedback";
    }

    @GetMapping("/student/query/ask")
    public String showQueryForm(Model model, Authentication auth) {
        addLoggedInUserToModel(model, auth);
        model.addAttribute("currentPath", "/student/query/ask");
        return "student-ask-query";
    }

    @PostMapping("/student/query/ask")
    public String submitQuery(@RequestParam String subject,
                              @RequestParam String question,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {

        User student = userRepository.findByEmail(principal.getName()).orElse(null);
        if (student == null) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/student/query/ask";
        }

        StudentQuery query = new StudentQuery();
        query.setUser(student);
        query.setSubject(subject);
        query.setQuestion(question);
        query.setStatus("Pending");
        query.setSubmittedAt(LocalDateTime.now());

        studentQueryRepository.save(query);
        redirectAttributes.addFlashAttribute("message", "Your question has been submitted.");

        return "redirect:/student/query/ask";
    }

    @GetMapping("/student/queries")
    public String viewMyQueries(Model model, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return "redirect:/login";

        User user = getCurrentUser(auth);
        List<StudentQuery> queries = studentQueryRepository.findByUser(user);

        model.addAttribute("queries", queries);
        model.addAttribute("currentPath", "/student/queries");
        addLoggedInUserToModel(model, auth);
        return "student-queries";
    }
    @PostMapping("/clear-notice")
    public String clearFlashNotice(Principal principal, HttpServletRequest request) {
        userRepository.findByEmail(principal.getName()).ifPresent(user -> {
            user.setFlashNotice(null);
            userRepository.save(user);
        });
        return "redirect:" + request.getHeader("Referer");
    }
}
