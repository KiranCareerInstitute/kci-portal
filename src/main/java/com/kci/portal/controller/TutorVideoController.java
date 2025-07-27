package com.kci.portal.controller;

import com.kci.portal.model.Assignment;
import com.kci.portal.model.StudentDoubt;
import com.kci.portal.model.User;
import com.kci.portal.model.VideoFeedback;
import com.kci.portal.model.VideoSolution;
import com.kci.portal.repository.AssignmentRepository;
import com.kci.portal.repository.StudentDoubtRepository;
import com.kci.portal.repository.UserRepository;
import com.kci.portal.repository.VideoSolutionRepository;
import com.kci.portal.service.VideoFeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/tutor/videos")
@PreAuthorize("hasRole('TUTOR')")
public class TutorVideoController {

    @Autowired private VideoSolutionRepository   videoSolutionRepository;
    @Autowired private AssignmentRepository      assignmentRepository;
    @Autowired private StudentDoubtRepository   studentDoubtRepository;
    @Autowired private UserRepository            userRepository;
    @Autowired private VideoFeedbackService      videoFeedbackService;

    /** Physical directory where videos are stored */
    @Value("${upload.path.videos}")
    private String videosUploadPath;

    /** URL path your ResourceHandler serves from that folder */
    private static final String UPLOAD_URL_PREFIX = "/uploads/videos";

    // ─── Show upload form + list of this tutor’s videos ─────────────────────
    @GetMapping
    public String showVideoUploadPage(Model model, Principal principal) {
        User tutor = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Tutor not found"));
        List<VideoSolution> videos     = videoSolutionRepository.findByUploadedBy(tutor);
        List<Assignment>    assignments = assignmentRepository.findAllReviewedAssignments();
        List<StudentDoubt>  doubts      = studentDoubtRepository.findAll();

        // Decorate doubts for display
        doubts.forEach(d -> {
            String name = (d.getUser() != null && d.getUser().getFullName() != null)
                    ? d.getUser().getFullName()
                    : "Unknown";
            d.setDisplayTitle(d.getTitle() + " – " + name);
        });

        model.addAttribute("video",           new VideoSolution());
        model.addAttribute("videos",          videos);
        model.addAttribute("assignments",     assignments);
        model.addAttribute("doubts",          doubts);
        model.addAttribute("uploadUrlPrefix", UPLOAD_URL_PREFIX);
        return "tutor/tutor-video-solutions";
    }

    // ─── Handle new upload ─────────────────────────────────────────────────
    @PostMapping("/upload")
    public String uploadVideoSolution(
            @RequestParam("title")       String        title,
            @RequestParam("description") String        description,
            @RequestParam(name="assignmentId", required=false) Long   assignmentId,
            @RequestParam(name="doubtId",      required=false) Long   doubtId,
            @RequestParam("videoFile") MultipartFile        videoFile,
            Principal                           principal
    ) throws IOException {
        VideoSolution vs = new VideoSolution();
        vs.setTitle(title);
        vs.setDescription(description);
        vs.setCreatedAt(LocalDateTime.now());

        // Link assignment
        if (assignmentId != null) {
            assignmentRepository.findById(assignmentId)
                    .ifPresent(vs::setAssignment);
        }
        // Link doubt (and student)
        if (doubtId != null) {
            studentDoubtRepository.findById(doubtId).ifPresent(d -> {
                vs.setDoubt(d);
                if (d.getUser() != null) {
                    vs.setStudent(d.getUser());
                }
            });
        }

        // Store file
        if (!videoFile.isEmpty()) {
            Path uploadDir = Paths.get(videosUploadPath)
                    .toAbsolutePath()
                    .normalize();
            Files.createDirectories(uploadDir);

            String originalName = StringUtils.cleanPath(videoFile.getOriginalFilename());
            String uniqueName   = System.currentTimeMillis()
                    + "_" + UUID.randomUUID()
                    + "_" + originalName;

            Path target = uploadDir.resolve(uniqueName);
            videoFile.transferTo(target.toFile());
            vs.setVideoPath(uniqueName);
        }

        // Mark uploader
        userRepository.findByEmail(principal.getName())
                .ifPresent(vs::setUploadedBy);

        videoSolutionRepository.save(vs);
        return "redirect:/tutor/videos";
    }

    @GetMapping("/tutor-video-feedback/{videoId}")
    public String viewFeedbackForVideo(@PathVariable Long videoId, Model model) {
        VideoSolution video = videoSolutionRepository.findById(videoId).orElseThrow();
        model.addAttribute("video", video);
        model.addAttribute("feedbacks", video.getFeedbackList());
        return "tutor/tutor_video_feedback_detail";
    }
    @PostMapping("/feedback/respond/{id}")
    public String respondToFeedback(@PathVariable Long id,
                                    @RequestParam("tutorResponse") String response,
                                    Principal principal) {
        // Load the tutor
        User tutor = userRepository.findByEmail(principal.getName()).orElseThrow();

        // Find feedback and update response
        VideoFeedback feedback = videoFeedbackService.findById(id);
        feedback.setTutorResponse(response);
        videoFeedbackService.saveFeedback(feedback);

        // Redirect back to the video feedback page
        Long videoId = feedback.getVideoSolution().getId();
        return "redirect:/tutor/videos/tutor-video-feedback/" + videoId;
    }
}
