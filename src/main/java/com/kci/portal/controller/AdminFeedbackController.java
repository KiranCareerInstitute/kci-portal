package com.kci.portal.controller;

import com.kci.portal.model.SessionFeedback;
import com.kci.portal.model.SessionSlot;
import com.kci.portal.repository.SessionFeedbackRepository;
import com.kci.portal.service.SessionFeedbackService;
import com.kci.portal.service.SessionSlotService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/feedback")
@PreAuthorize("hasRole('ADMIN')")
public class AdminFeedbackController {

    @Autowired
    private SessionFeedbackRepository feedbackRepo;

    @Autowired
    private SessionSlotService slotService;

    @Autowired
    private SessionFeedbackService feedbackService;

    // ✅ Show form to edit tutor reply
    @GetMapping("/edit-reply/{slotId}")
    public String showEditReplyForm(@PathVariable Long slotId, Model model) {
        List<SessionFeedback> feedbackList = feedbackRepo.findBySessionSlotIdAndSubmittedBy(slotId, "STUDENT");
        if (feedbackList.isEmpty()) {
            return "redirect:/admin/dashboard?noStudentFeedback";
        }
        SessionFeedback feedback = feedbackList.get(feedbackList.size() - 1);

        model.addAttribute("feedback", feedback);
        model.addAttribute("slot", feedback.getSessionSlot());
        return "admin/edit-feedback-reply";
    }

    // ✅ Save updated tutor reply
    @PostMapping("/edit-reply/{slotId}")
    public String saveEditedReply(@PathVariable Long slotId,
                                  @RequestParam("tutorReplyText") String replyText) {
        List<SessionFeedback> feedbackList = feedbackRepo.findBySessionSlotIdAndSubmittedBy(slotId, "STUDENT");
        if (feedbackList.isEmpty()) {
            return "redirect:/admin/dashboard?error=FeedbackNotFound";
        }
        SessionFeedback feedback = feedbackList.get(feedbackList.size() - 1);
        feedback.setTutorReplyText(replyText);
        feedback.setRepliedAt(LocalDateTime.now());
        feedbackRepo.save(feedback);

        return "redirect:/admin/feedback/edit-reply/" + slotId + "?success";
    }

    // ✅ View all feedbacks (list view)
    @GetMapping("/list")
    public String viewAllFeedbacks(Model model) {
        List<SessionFeedback> allFeedbacks = feedbackRepo.findAll();
        model.addAttribute("feedbackList", allFeedbacks);
        return "admin/admin-feedback-list";
    }

    // ✅ View feedback for a specific session (student + tutor)
    @GetMapping("/view/{slotId}")
    public String adminViewFeedback(@PathVariable Long slotId, Model model) {
        SessionSlot slot = slotService.getSlotById(slotId);
        if (slot == null) return "redirect:/admin/feedback/list";

        List<SessionFeedback> studentFeedbackList = feedbackRepo.findBySessionSlotIdAndSubmittedBy(slotId, "STUDENT");
        List<SessionFeedback> tutorFeedbackList = feedbackRepo.findBySessionSlotIdAndSubmittedBy(slotId, "TUTOR");

        SessionFeedback studentFeedback = studentFeedbackList.isEmpty() ? null : studentFeedbackList.get(studentFeedbackList.size() - 1);
        SessionFeedback tutorFeedback = tutorFeedbackList.isEmpty() ? null : tutorFeedbackList.get(tutorFeedbackList.size() - 1);

        model.addAttribute("slot", slot);
        model.addAttribute("studentFeedback", studentFeedback);
        model.addAttribute("tutorFeedback", tutorFeedback);
        model.addAttribute("noFeedback", studentFeedback == null && tutorFeedback == null);

        return "admin/admin-view-feedback";
    }

    // ✅ Allow admin to download PDF summary
    @GetMapping("/pdf/{slotId}")
    public void downloadFeedbackPdfAdmin(@PathVariable Long slotId, HttpServletResponse response) {
        feedbackService.generateFeedbackPdf(slotId, response); // Uses common service method
    }

    // ✅ Allow admin to download tutor's uploaded file (e.g. PDF/image)
    @GetMapping("/download/{slotId}")
    public void downloadTutorAttachment(@PathVariable Long slotId, HttpServletResponse response) throws IOException {
        List<SessionFeedback> feedbackList = feedbackRepo.findBySessionSlotIdAndSubmittedBy(slotId, "TUTOR");
        if (feedbackList.isEmpty() || feedbackList.get(feedbackList.size() - 1).getAttachmentFilePath() == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
            return;
        }

        SessionFeedback feedback = feedbackList.get(feedbackList.size() - 1);
        Path file = Paths.get(feedback.getAttachmentFilePath());

        if (!Files.exists(file)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found on disk");
            return;
        }

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" + feedback.getAttachmentFileName());
        Files.copy(file, response.getOutputStream());
        response.getOutputStream().flush();
    }
}
