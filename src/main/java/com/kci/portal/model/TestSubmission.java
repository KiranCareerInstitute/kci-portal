package com.kci.portal.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "test_submissions")
public class TestSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "marks")
    private Integer marks;

    @Column(name = "feedback")
    private String feedback;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "test_id")
    private Test test;

    private String fileName;

    private LocalDateTime submittedAt;

    @Column(length=1024)
    private String studentFeedback;

    public String getStudentFeedback() { return studentFeedback; }
    public void setStudentFeedback(String studentFeedback) { this.studentFeedback = studentFeedback; }

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    @Column(name = "last_reviewed_by")
    private String lastReviewedBy;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    public String getLastReviewedBy() { return lastReviewedBy; }
    public void setLastReviewedBy(String s) { this.lastReviewedBy = s; }
    public LocalDateTime getLastReviewedAt() { return lastReviewedAt; }
    public void setLastReviewedAt(LocalDateTime t) { this.lastReviewedAt = t; }

    @Column(length = 1024)
    private String tutorReply;

    @Column(length = 1024)
    private String adminReply;

    public String getTutorReply() { return tutorReply; }
    public void setTutorReply(String tutorReply) { this.tutorReply = tutorReply; }

    public String getAdminReply() { return adminReply; }
    public void setAdminReply(String adminReply) { this.adminReply = adminReply; }
    // In TestSubmission.java
    @Column(name = "admin_feedback")
    private String adminFeedback;

    public String getAdminFeedback() { return adminFeedback; }
    public void setAdminFeedback(String adminFeedback) { this.adminFeedback = adminFeedback; }

    // Getters & Setters
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }

    public void setUser(User user) { this.user = user; }

    public Test getTest() { return test; }

    public void setTest(Test test) { this.test = test; }

    public String getFileName() { return fileName; }

    public void setFileName(String fileName) { this.fileName = fileName; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }

    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    //getters and setters

    public Integer getMarks() {
        return marks;
    }

    public void setMarks(Integer marks) {
        this.marks = marks;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

}
