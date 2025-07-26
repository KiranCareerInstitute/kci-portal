package com.kci.portal.controller;

import com.kci.portal.model.SessionFeedback;
import com.kci.portal.model.SessionSlot;
import com.kci.portal.repository.SessionFeedbackRepository;
import com.kci.portal.service.PdfExportService;
import com.kci.portal.service.SessionSlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/tutor/feedback")
@PreAuthorize("hasRole('TUTOR')")
public class TutorFeedbackController {

    @Autowired
    private SessionSlotService slotService;

    @Autowired
    private SessionFeedbackRepository feedbackRepo;

    @Autowired
    private PdfExportService pdfExportService;

    private final Path uploadDir = Paths.get("uploads/session-feedback");

    // ✅ 1. GET: Tutor feedback form
    @GetMapping("/{slotId}")
    public String feedbackForm(@PathVariable Long slotId, Model model) {
        SessionSlot slot = slotService.getSlotById(slotId);
        if (slot == null || !slot.isBooked()) {
            return "redirect:/tutor/my-slots";
        }

        model.addAttribute("slot", slot);
        model.addAttribute("feedback", new SessionFeedback());
        return "tutor/session-feedback-form";
    }

    // ✅ 2. POST: Tutor submits feedback
    @PostMapping("/{slotId}")
    public String submitFeedback(
            @PathVariable Long slotId,
            @ModelAttribute SessionFeedback feedback,
            @RequestParam("file") MultipartFile file,
            Principal principal,
            Model model
    ) {
        SessionSlot slot = slotService.getSlotById(slotId);
        if (slot == null || !slot.isBooked()) return "redirect:/tutor/my-slots";

        try {
            feedback.setSessionSlot(slot);
            feedback.setTutorEmail(principal.getName());
            feedback.setStudentEmail(slot.getStudentEmail());
            feedback.setSubmittedAt(LocalDateTime.now());

            if (!file.isEmpty()) {
                Files.createDirectories(uploadDir);
                String filename = System.currentTimeMillis() + "_" + StringUtils.cleanPath(file.getOriginalFilename());
                Path filePath = uploadDir.resolve(filename);
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                feedback.setAttachmentFileName(file.getOriginalFilename());
                feedback.setAttachmentFilePath(filePath.toString());
            }

            feedback.setSubmittedBy("TUTOR");
            feedbackRepo.save(feedback);

            // Add the success message!
            model.addAttribute("successMessage", "Feedback submitted successfully!");

        } catch (Exception e) {
            model.addAttribute("errorMessage", "An error occurred. Please try again.");
        }

        // Add slot and feedback back for the form to re-render
        model.addAttribute("slot", slot);
        model.addAttribute("feedback", new SessionFeedback()); // or feedback, to show what user entered

        return "tutor/session-feedback-form";
    }

    // ✅ 3. GET: View student feedback
    @GetMapping("/view-student/{slotId}")
    public String viewStudentFeedback(@PathVariable Long slotId, Model model, Principal principal) {
        SessionSlot slot = slotService.getSlotById(slotId);

        if (slot == null || !slot.getTutorEmail().equals(principal.getName())) {
            return "redirect:/tutor/my-slots";
        }

        List<SessionFeedback> feedbackList = feedbackRepo.findBySessionSlotIdAndSubmittedBy(slotId, "STUDENT");
        if (feedbackList.isEmpty()) {
            model.addAttribute("noFeedback", true);
        } else {
            // Pick the latest feedback (or whichever logic you want)
            SessionFeedback feedback = feedbackList.get(feedbackList.size() - 1);
            model.addAttribute("feedback", feedback);
        }

        model.addAttribute("slot", slot);
        return "tutor/view-student-feedback";
    }

    // ✅ 4. GET: Show reply form
    @GetMapping("/reply-feedback/{slotId}")
    public String showReplyForm(@PathVariable Long slotId, Model model, Principal principal) {
        SessionSlot slot = slotService.getSlotById(slotId);
        if (slot == null || !slot.getTutorEmail().equals(principal.getName())) {
            return "redirect:/tutor/my-slots";
        }

        List<SessionFeedback> feedbackList = feedbackRepo.findBySessionSlotIdAndSubmittedBy(slotId, "STUDENT");
        if (feedbackList.isEmpty()) {
            return "redirect:/tutor/view-student/" + slotId + "?noFeedback";
        }

        SessionFeedback feedback = feedbackList.get(feedbackList.size() - 1);
        model.addAttribute("slot", slot);
        model.addAttribute("feedback", feedback);
        return "tutor/student-feedback-reply-form";
    }

    // ✅ 5. POST: Save tutor reply to student feedback
    @PostMapping("/reply-feedback/{slotId}")
    public String replyToStudentFeedback(@PathVariable Long slotId,
                                         @RequestParam("tutorReplyText") String tutorReplyText,
                                         Principal principal) {

        List<SessionFeedback> feedbackList = feedbackRepo.findBySessionSlotIdAndSubmittedBy(slotId, "STUDENT");

        if (feedbackList.isEmpty()) {
            return "redirect:/tutor/my-slots?error=NoFeedbackFound";
        }

        SessionFeedback feedback = feedbackList.get(feedbackList.size() - 1);

        if (!feedback.getTutorEmail().equals(principal.getName())) {
            return "redirect:/tutor/my-slots?error=AccessDenied";
        }

        feedback.setTutorReplyText(tutorReplyText);
        feedback.setRepliedAt(LocalDateTime.now());
        feedbackRepo.save(feedback);

        return "redirect:/tutor/feedback/view-student/" + slotId + "?replySuccess";
    }

    // ✅ 6. GET: Download feedback PDF
    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        SessionFeedback feedback = feedbackRepo.findById(id).orElse(null);
        if (feedback == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] pdfBytes = pdfExportService.generateSessionSummaryPdf(feedback);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=session-summary.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
