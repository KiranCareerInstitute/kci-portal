package com.kci.portal.controller;

import com.kci.portal.model.StudentDoubt;
import com.kci.portal.model.User;
import com.kci.portal.repository.StudentDoubtRepository;
import com.kci.portal.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/admin/doubts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDoubtController {

    @Autowired
    private StudentDoubtRepository doubtRepository;

    @Autowired
    private UserRepository userRepository;

    /** same folder students use */
    @Value("${upload.path.doubts}")
    private String doubtUploadDir;

    /** URL prefix for your static‐resource handler */
    private static final String UPLOAD_URL_PREFIX = "/uploads/doubts";

    // ─── List & filter ───────────────────────────────────────────────────────
    @GetMapping
    public String viewAllDoubts(@RequestParam(name = "status",     required = false, defaultValue = "ALL")  String status,
                                @RequestParam(name = "assignment", required = false, defaultValue = "ALL")  String assignment,
                                Model model) {

        List<StudentDoubt> doubts;

        if (status.equals("ALL") && assignment.equals("ALL")) {
            doubts = doubtRepository.findAllByOrderBySubmittedAtDesc();
        } else if (!status.equals("ALL") && assignment.equals("ALL")) {
            doubts = doubtRepository.findByStatusOrderBySubmittedAtDesc(status);
        } else if (status.equals("ALL") && assignment.equals("UNASSIGNED")) {
            doubts = doubtRepository.findByAssignedTutorIsNullOrderBySubmittedAtDesc();
        } else if (status.equals("ALL") && assignment.equals("ASSIGNED")) {
            doubts = doubtRepository.findByAssignedTutorIsNotNullOrderBySubmittedAtDesc();
        } else {
            if (assignment.equals("UNASSIGNED")) {
                doubts = doubtRepository
                        .findByStatusAndAssignedTutorIsNullOrderBySubmittedAtDesc(status);
            } else {
                doubts = doubtRepository
                        .findByStatusAndAssignedTutorIsNotNullOrderBySubmittedAtDesc(status);
            }
        }

        // force‐load tutor to avoid LazyInitializationException
        for (StudentDoubt d : doubts) {
            if (d.getAssignedTutor() != null) d.getAssignedTutor().getName();
        }

        model.addAttribute("doubts",           doubts);
        model.addAttribute("tutors",           userRepository.findByRole("ROLE_TUTOR"));
        model.addAttribute("selectedStatus",   status);
        model.addAttribute("selectedAssignment", assignment);
        model.addAttribute("uploadUrlPrefix",  UPLOAD_URL_PREFIX);
        return "admin/admin-doubt-list";
    }

    // ─── Review form ────────────────────────────────────────────────────────
    @GetMapping("/review/{id}")
    public String reviewDoubt(@PathVariable Long id, Model model) {
        StudentDoubt doubt = doubtRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Doubt not found"));
        model.addAttribute("doubt",          doubt);
        model.addAttribute("tutors",         userRepository.findByRole("ROLE_TUTOR"));
        model.addAttribute("uploadUrlPrefix", UPLOAD_URL_PREFIX);
        return "admin/admin-doubt-review";
    }

    // ─── Handle feedback + optional solution file ────────────────────────────
    @PostMapping("/review/{id}")
    public String handleReview(@PathVariable Long id,
                               @RequestParam("feedbackText")   String         feedbackText,
                               @RequestParam("solutionFile")   MultipartFile  solutionFile,
                               @RequestParam("assignedTutorId") Long          assignedTutorId,
                               RedirectAttributes             redirectAttributes) {

        try {
            StudentDoubt doubt = doubtRepository
                    .findById(id)
                    .orElseThrow(() -> new RuntimeException("Doubt not found"));

            // assign tutor
            Optional<User> tutorOpt = userRepository.findById(assignedTutorId);
            tutorOpt.ifPresent(doubt::setAssignedTutor);

            // ── REVISED UPLOAD BLOCK ─────────────────────────────────────────────
            if (!solutionFile.isEmpty()) {
                // 1) resolve & create directories
                Path uploadPath = Paths.get(doubtUploadDir)
                        .toAbsolutePath()
                        .normalize();
                Files.createDirectories(uploadPath);

                // 2) build a safe, unique filename
                String originalName = StringUtils.cleanPath(solutionFile.getOriginalFilename());
                String uniqueName   = System.currentTimeMillis()
                        + "_"
                        + UUID.randomUUID()
                        + "_"
                        + originalName;

                // 3) copy file to target
                Path targetLocation = uploadPath.resolve(uniqueName);
                solutionFile.transferTo(targetLocation.toFile());

                // 4) persist into entity
                doubt.setSolutionFileName(originalName);
                doubt.setSolutionFilePath(uniqueName);
            }
            // ─────────────────────────────────────────────────────────────────────

            // feedback & resolve
            doubt.setFeedbackText(feedbackText);
            doubt.setResolvedAt(LocalDateTime.now());
            doubt.setStatus("SOLVED");
            doubtRepository.save(doubt);

            redirectAttributes.addFlashAttribute("success",
                    "✅ Feedback submitted and tutor assigned.");
        } catch (IOException e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error",
                    "❌ Failed to upload solution file: " + e.getMessage());
        }

        return "redirect:/admin/doubts/review/" + id;
    }
    // ─── Inline assignment from the list page ────────────────────────────────
    @PostMapping("/assign/{id}")
    public String assignTutor(@PathVariable Long id,
                              @RequestParam("tutorId") Long tutorId,
                              RedirectAttributes redirectAttributes) {

        StudentDoubt doubt = doubtRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Doubt not found"));
        User tutor = userRepository
                .findById(tutorId)
                .orElseThrow(() -> new RuntimeException("Tutor not found"));

        doubt.setAssignedTutor(tutor);
        if (doubt.getStatus() == null || doubt.getStatus().equalsIgnoreCase("PENDING")) {
            doubt.setStatus("ASSIGNED");
        }
        doubtRepository.save(doubt);

        redirectAttributes.addFlashAttribute("success",
                "✅ Tutor assigned successfully.");
        return "redirect:/admin/doubts";
    }

}
