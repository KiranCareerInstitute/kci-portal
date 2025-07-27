package com.kci.portal.controller;

import com.kci.portal.model.StudentDoubt;
import com.kci.portal.repository.StudentDoubtRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/tutor/doubts")
@PreAuthorize("hasAnyRole('TUTOR','ADMIN')")
public class TutorDoubtController {

    @Autowired
    private StudentDoubtRepository studentDoubtRepository;

    /** same folder students use */
    @Value("${upload.path.doubts}")
    private String doubtUploadDir;

    /** URL prefix (needs to match your static‐resource handler) */
    private static final String UPLOAD_URL_PREFIX = "/uploads/doubts";

    // ─── List only assigned doubts ─────────────────────────────────────────────
    @GetMapping
    public String listAssignedDoubts(Model model, Principal principal) {
        String tutorEmail = principal.getName();
        List<StudentDoubt> doubts = studentDoubtRepository
                .findByAssignedTutor_EmailOrderBySubmittedAtDesc(tutorEmail);

        model.addAttribute("doubts",           doubts);
        model.addAttribute("uploadUrlPrefix",  UPLOAD_URL_PREFIX);
        return "tutor/tutor-doubt-list";
    }

    // ─── Show review form ──────────────────────────────────────────────────────
    @GetMapping("/review/{id}")
    public String reviewDoubt(@PathVariable Long id,
                              Principal principal,
                              Model model,
                              RedirectAttributes redirectAttributes) {

        StudentDoubt doubt = studentDoubtRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Doubt not found"));

        if (doubt.getAssignedTutor() == null
                || !doubt.getAssignedTutor().getEmail().equals(principal.getName())) {

            redirectAttributes.addFlashAttribute("error",
                    "⚠️ You are not authorized to view this doubt.");
            return "redirect:/tutor/doubts";
        }

        model.addAttribute("doubt",           doubt);
        model.addAttribute("uploadUrlPrefix", UPLOAD_URL_PREFIX);
        return "tutor/tutor-doubt-review";
    }

    // ─── Handle tutor feedback & optional solution upload ─────────────────────
    @PostMapping("/review/{id}")
    public String handleReview(@PathVariable Long id,
                               @RequestParam("feedback")   String        feedback,
                               @RequestParam(value="solutionFile", required=false)
                               MultipartFile solutionFile,
                               Principal                            principal,
                               RedirectAttributes                   redirectAttributes) {

        try {
            StudentDoubt doubt = studentDoubtRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Doubt not found"));

            if (doubt.getAssignedTutor() == null
                    || !doubt.getAssignedTutor().getEmail().equals(principal.getName())) {

                redirectAttributes.addFlashAttribute("error",
                        "❌ You are not authorized to submit feedback.");
                return "redirect:/tutor/doubts";
            }

            // ── UPLOAD SOLUTION ─────────────────────────────────────────────
            if (solutionFile != null && !solutionFile.isEmpty()) {
                // 1) ensure folder exists
                Path uploadPath = Paths.get(doubtUploadDir)
                        .toAbsolutePath()
                        .normalize();
                Files.createDirectories(uploadPath);

                // 2) build a unique, safe filename
                String originalName = StringUtils.cleanPath(
                        solutionFile.getOriginalFilename());
                String uniqueName = System.currentTimeMillis()
                        + "_" + UUID.randomUUID()
                        + "_" + originalName;

                // 3) copy file
                Path targetLocation = uploadPath.resolve(uniqueName);
                solutionFile.transferTo(targetLocation.toFile());

                // 4) persist metadata
                doubt.setSolutionFileName(originalName);
                doubt.setSolutionFilePath(uniqueName);
            }
            // ────────────────────────────────────────────────────────────────

            // save feedback
            doubt.setFeedbackText(feedback);
            doubt.setResolvedAt(LocalDateTime.now());
            doubt.setStatus("SOLVED");
            studentDoubtRepository.save(doubt);

            redirectAttributes.addFlashAttribute("success",
                    "✅ Doubt marked as solved.");
        } catch (IOException e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error",
                    "❌ Failed to upload solution: " + e.getMessage());
        }

        return "redirect:/tutor/doubts/review/" + id;
    }
}
