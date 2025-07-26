package com.kci.portal.controller;

import com.kci.portal.model.SessionFeedback;
import com.kci.portal.model.SessionSlot;
import com.kci.portal.repository.SessionFeedbackRepository;
import com.kci.portal.service.PdfExportService;
import com.kci.portal.service.SessionSlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.file.*;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/student/feedback")
@PreAuthorize("hasRole('USER')")
public class StudentFeedbackController {

    @Autowired
    private SessionFeedbackRepository feedbackRepo;

    @Autowired
    private SessionSlotService slotService;

    @Autowired
    private PdfExportService pdfExportService;

    // ✅ 1. View feedback given by tutor
    @GetMapping("/{slotId}")
    public String viewFeedback(@PathVariable Long slotId, Model model, Principal principal) {
        SessionSlot slot = slotService.getSlotById(slotId);
        if (slot == null || !slot.isBooked() || !slot.getStudentEmail().equals(principal.getName())) {
            return "redirect:/student/book-session";
        }

        List<SessionFeedback> studentFeedbackList = feedbackRepo.findBySessionSlotIdAndSubmittedBy(slotId, "STUDENT");
        List<SessionFeedback> tutorFeedbackList = feedbackRepo.findBySessionSlotIdAndSubmittedBy(slotId, "TUTOR");

        SessionFeedback studentFeedback = studentFeedbackList.isEmpty() ? null : studentFeedbackList.get(studentFeedbackList.size() - 1);
        SessionFeedback tutorFeedback = tutorFeedbackList.isEmpty() ? null : tutorFeedbackList.get(tutorFeedbackList.size() - 1);

        model.addAttribute("slot", slot);

        if (studentFeedback != null) {
            model.addAttribute("studentFeedback", studentFeedback);
        }

        if (tutorFeedback != null) {
            model.addAttribute("tutorFeedback", tutorFeedback);
        }

        if (studentFeedback == null && tutorFeedback == null) {
            model.addAttribute("noFeedback", true);
        }

        return "student/view-feedback";
    }

    // ✅ 2. Download tutor feedback attachment
    @GetMapping("/download/{slotId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long slotId, Principal principal) {
        List<SessionFeedback> feedbackList = feedbackRepo.findBySessionSlotIdAndSubmittedBy(slotId, "TUTOR");
        if (feedbackList.isEmpty()) return ResponseEntity.notFound().build();
        SessionFeedback feedback = feedbackList.get(feedbackList.size() - 1);

        if (!feedback.getStudentEmail().equals(principal.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Path filePath = Paths.get(feedback.getAttachmentFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) return ResponseEntity.notFound().build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + feedback.getAttachmentFileName() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ✅ 3. Download feedback as PDF summary
    @GetMapping("/pdf/{slotId}")
    public ResponseEntity<byte[]> downloadFeedbackPdf(@PathVariable Long slotId, Principal principal) {
        List<SessionFeedback> feedbackList = feedbackRepo.findBySessionSlotIdAndSubmittedBy(slotId, "TUTOR");
        if (feedbackList.isEmpty()) return ResponseEntity.notFound().build();
        SessionFeedback feedback = feedbackList.get(feedbackList.size() - 1);

        if (!feedback.getStudentEmail().equals(principal.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        byte[] pdfBytes = pdfExportService.generateSessionSummaryPdf(feedback);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=session-feedback.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    // ✅ 4. Show form to submit feedback to tutor
    @GetMapping("/submit/{slotId}")
    public String showStudentFeedbackForm(@PathVariable Long slotId, Model model, Principal principal) {
        SessionSlot slot = slotService.getSlotById(slotId);
        if (slot == null || !slot.isBooked() || !slot.getStudentEmail().equals(principal.getName())) {
            return "redirect:/student/book-session";
        }

        model.addAttribute("slot", slot);
        model.addAttribute("feedback", new SessionFeedback());
        return "student/student-feedback-form";
    }

    // ✅ 5. Submit feedback (text only) to tutor
    @PostMapping("/submit/{slotId}")
    public String submitStudentFeedback(@PathVariable Long slotId,
                                        @ModelAttribute SessionFeedback feedback,
                                        Principal principal) {
        SessionSlot slot = slotService.getSlotById(slotId);
        if (slot == null || !slot.getStudentEmail().equals(principal.getName())) {
            return "redirect:/student/book-session";
        }

        feedback.setSessionSlot(slot);
        feedback.setStudentEmail(principal.getName());
        feedback.setTutorEmail(slot.getTutorEmail());
        feedback.setSubmittedAt(LocalDateTime.now());
        feedback.setSubmittedBy("STUDENT");

        feedbackRepo.save(feedback);
        return "redirect:/student/book-session?studentFeedbackSuccess";
    }
}
