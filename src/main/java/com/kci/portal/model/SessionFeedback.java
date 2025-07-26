package com.kci.portal.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "session_feedback")
public class SessionFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_slot_id", nullable = false)
    private SessionSlot sessionSlot;

    private String tutorEmail;
    private String studentEmail;

    @Column(columnDefinition = "TEXT")
    private String feedbackText;

    private String attachmentFileName;
    private String attachmentFilePath;

    private LocalDateTime submittedAt;

    private String submittedBy; // "STUDENT" or "TUTOR"

    // ✅ New fields for tutor reply
    @Column(columnDefinition = "TEXT")
    private String tutorReplyText;

    private LocalDateTime repliedAt;

    // Getters and Setters...

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SessionSlot getSessionSlot() {
        return sessionSlot;
    }

    public void setSessionSlot(SessionSlot sessionSlot) {
        this.sessionSlot = sessionSlot;
    }

    public String getTutorEmail() {
        return tutorEmail;
    }

    public void setTutorEmail(String tutorEmail) {
        this.tutorEmail = tutorEmail;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public String getFeedbackText() {
        return feedbackText;
    }

    public void setFeedbackText(String feedbackText) {
        this.feedbackText = feedbackText;
    }

    public String getAttachmentFileName() {
        return attachmentFileName;
    }

    public void setAttachmentFileName(String attachmentFileName) {
        this.attachmentFileName = attachmentFileName;
    }

    public String getAttachmentFilePath() {
        return attachmentFilePath;
    }

    public void setAttachmentFilePath(String attachmentFilePath) {
        this.attachmentFilePath = attachmentFilePath;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(String submittedBy) {
        this.submittedBy = submittedBy;
    }

    public String getTutorReplyText() {
        return tutorReplyText;
    }

    public void setTutorReplyText(String tutorReplyText) {
        this.tutorReplyText = tutorReplyText;
    }

    public LocalDateTime getRepliedAt() {
        return repliedAt;
    }

    public void setRepliedAt(LocalDateTime repliedAt) {
        this.repliedAt = repliedAt;
    }
}
