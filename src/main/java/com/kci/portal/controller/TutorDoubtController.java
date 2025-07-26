package com.kci.portal.controller;

import com.kci.portal.model.StudentDoubt;
import com.kci.portal.repository.StudentDoubtRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
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
import java.util.UUID;

@Controller
@RequestMapping("/tutor/doubts")
@PreAuthorize("hasAnyRole('TUTOR', 'ADMIN')")
public class TutorDoubtController {

    @Autowired
    private StudentDoubtRepository studentDoubtRepository;

    @Value("${upload.path.doubts}")
    private String doubtUploadPath;


    // ✅ Show only doubts assigned to the logged-in tutor
    @GetMapping
    public String listAssignedDoubts(Model model, Principal principal) {
        String tutorEmail = principal.getName(); // Logged-in tutor email
        System.out.println("Tutor Email: " + tutorEmail);
        List<StudentDoubt> doubts = studentDoubtRepository.findByAssignedTutor_EmailOrderBySubmittedAtDesc(tutorEmail);
        System.out.println("Assigned Doubts Count: " + doubts.size());
        model.addAttribute("doubts", doubts);
        return "tutor/tutor-doubt-list";
    }

    // ✅ Show review form (only allow if doubt is assigned to this tutor)
    @GetMapping("/review/{id}")
    public String reviewDoubt(@PathVariable Long id, Principal principal, Model model, RedirectAttributes redirectAttributes) {
        StudentDoubt doubt = studentDoubtRepository.findById(id).orElseThrow();
        String tutorEmail = principal.getName();

        if (doubt.getAssignedTutor() == null || !doubt.getAssignedTutor().getEmail().equals(tutorEmail)) {
            redirectAttributes.addFlashAttribute("error", "⚠️ You are not authorized to review this doubt.");
            return "redirect:/tutor/doubts";
        }

        model.addAttribute("doubt", doubt);
        return "tutor/tutor-doubt-review";
    }

    // ✅ Handle tutor's feedback
    @PostMapping("/review/{id}")
    public String handleReview(@PathVariable Long id,
                               @RequestParam("feedback") String feedback,
                               @RequestParam(value = "solutionFile", required = false) MultipartFile solutionFile,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        try {
            StudentDoubt doubt = studentDoubtRepository.findById(id).orElseThrow();
            String tutorEmail = principal.getName();

            if (doubt.getAssignedTutor() == null || !doubt.getAssignedTutor().getEmail().equals(tutorEmail)) {
                redirectAttributes.addFlashAttribute("error", "❌ You are not authorized to submit feedback.");
                return "redirect:/tutor/doubts";
            }

            // Optional file upload
            if (solutionFile != null && !solutionFile.isEmpty()) {
                String originalFilename = solutionFile.getOriginalFilename();
                String uniqueName = UUID.randomUUID() + "_" + originalFilename;
                File dest = new File(doubtUploadPath + File.separator + uniqueName);
                solutionFile.transferTo(dest);

                doubt.setSolutionFileName(originalFilename);
                doubt.setSolutionFilePath(uniqueName);
            }

            doubt.setFeedbackText(feedback);
            doubt.setResolvedAt(LocalDateTime.now());
            doubt.setStatus("SOLVED");

            studentDoubtRepository.save(doubt);
            redirectAttributes.addFlashAttribute("success", "✅ Doubt marked as solved.");
        } catch (IOException e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "❌ Failed to upload solution.");
        }

        return "redirect:/tutor/doubts/review/" + id;
    }
}
