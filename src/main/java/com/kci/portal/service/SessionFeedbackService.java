package com.kci.portal.service;

import com.kci.portal.model.SessionFeedback;
import com.kci.portal.model.SessionSlot;
import com.kci.portal.repository.SessionFeedbackRepository;
import com.kci.portal.repository.SessionSlotRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class SessionFeedbackService {

    @Autowired
    private SessionFeedbackRepository feedbackRepo;

    @Autowired
    private SessionSlotRepository slotRepo;

    public void generateFeedbackPdf(Long slotId, HttpServletResponse response) {
        try {
            SessionSlot slot = slotRepo.findById(slotId).orElse(null);
            if (slot == null) return;

            // Updated: Get list, not Optional
            List<SessionFeedback> studentFeedbackList = feedbackRepo.findBySessionSlotIdAndSubmittedBy(slotId, "STUDENT");
            List<SessionFeedback> tutorFeedbackList = feedbackRepo.findBySessionSlotIdAndSubmittedBy(slotId, "TUTOR");

            // Use latest if multiple feedbacks (just like your controllers)
            SessionFeedback studentFeedback = studentFeedbackList.isEmpty() ? null : studentFeedbackList.get(studentFeedbackList.size() - 1);
            SessionFeedback tutorFeedback = tutorFeedbackList.isEmpty() ? null : tutorFeedbackList.get(tutorFeedbackList.size() - 1);

            // PDF setup
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=session-feedback-" + slotId + ".pdf");
            OutputStream out = response.getOutputStream();

            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

            document.add(new Paragraph("Session Feedback Summary", titleFont));
            document.add(new Paragraph(" "));

            // Session Info
            document.add(new Paragraph("Session ID: " + slot.getId(), textFont));
            document.add(new Paragraph("Tutor Email: " + slot.getTutorEmail(), textFont));
            document.add(new Paragraph("Student Email: " + slot.getStudentEmail(), textFont));
            document.add(new Paragraph("Date: " + slot.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), textFont));
            document.add(new Paragraph("Time: " + slot.getStartTime() + " - " + slot.getEndTime(), textFont));
            document.add(new Paragraph(" "));

            // Tutor Feedback
            if (tutorFeedback != null) {
                document.add(new Paragraph("Tutor's Feedback:", sectionFont));
                document.add(new Paragraph(tutorFeedback.getFeedbackText(), textFont));
                document.add(new Paragraph(" "));
            }

            // Student Feedback + Tutor Reply
            if (studentFeedback != null) {
                document.add(new Paragraph("Student's Feedback:", sectionFont));
                document.add(new Paragraph(studentFeedback.getFeedbackText(), textFont));

                if (studentFeedback.getTutorReplyText() != null) {
                    document.add(new Paragraph(" "));
                    document.add(new Paragraph("Tutor's Reply to Student:", sectionFont));
                    document.add(new Paragraph(studentFeedback.getTutorReplyText(), textFont));
                }
            }

            document.close();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
