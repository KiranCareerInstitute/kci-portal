package com.kci.portal.service;

import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.*;
import com.itextpdf.layout.element.*;
import com.kci.portal.model.SessionFeedback;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class PdfExportService {

    public byte[] generateSessionSummaryPdf(SessionFeedback feedback) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf);

        doc.add(new Paragraph("📄 Tutoring Session Summary").setBold().setFontSize(18));
        doc.add(new Paragraph("Tutor: " + feedback.getTutorEmail()));
        doc.add(new Paragraph("Student: " + feedback.getStudentEmail()));

        // ✅ Fixed line: Get SessionSlot ID properly
        if (feedback.getSessionSlot() != null) {
            doc.add(new Paragraph("Session ID: " + feedback.getSessionSlot().getId()));
        } else {
            doc.add(new Paragraph("Session ID: (not linked)"));
        }

        doc.add(new Paragraph("Date Submitted: " + feedback.getSubmittedAt()));
        doc.add(new Paragraph("\n📝 Feedback:").setBold());
        doc.add(new Paragraph(feedback.getFeedbackText()));

        doc.close();
        return baos.toByteArray();
    }
}
