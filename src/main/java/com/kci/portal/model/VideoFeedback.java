package com.kci.portal.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "video_feedback",
        uniqueConstraints = @UniqueConstraint(columnNames = {"video_solution_id", "student_id"})
)
public class VideoFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private VideoSolution videoSolution;

    @ManyToOne
    private User student;

    private String comment;

    private int rating;

    private LocalDateTime submittedAt = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String tutorResponse;

    @Column(columnDefinition = "TEXT")
    private String adminResponse;

    // Getters and Setters

    public Long getId() { return id; }

    public VideoSolution getVideoSolution() { return videoSolution; }
    public void setVideoSolution(VideoSolution videoSolution) { this.videoSolution = videoSolution; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public String getTutorResponse() {
        return tutorResponse;
    }

    public void setTutorResponse(String tutorResponse) {
        this.tutorResponse = tutorResponse;
    }

    public String getAdminResponse() {
        return adminResponse;
    }

    public void setAdminResponse(String adminResponse) {
        this.adminResponse = adminResponse;
    }
}
