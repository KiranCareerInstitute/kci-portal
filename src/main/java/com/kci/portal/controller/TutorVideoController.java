package com.kci.portal.controller;

import com.kci.portal.model.*;
import com.kci.portal.repository.StudentDoubtRepository;
import com.kci.portal.repository.VideoSolutionRepository;
import com.kci.portal.repository.AssignmentRepository;
import com.kci.portal.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import com.kci.portal.service.VideoFeedbackService;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/tutor/videos")
@PreAuthorize("hasRole('TUTOR')")
public class TutorVideoController {

    @Autowired
    private VideoSolutionRepository videoSolutionRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoFeedbackService videoFeedbackService;

    @Autowired
    private StudentDoubtRepository studentDoubtRepository;

    @Value("${upload.path.videos}")
    private String videosUploadPath;

    private final String uploadDir = System.getProperty("user.dir") + "/uploads/video-solutions/";

    // ✅ Main GET: shows upload form + list
    @GetMapping
    public String showVideoUploadPage(Model model, Principal principal) {
        List<VideoSolution> videos = videoSolutionRepository.findAll(); // Later: filter by principal
        List<Assignment> assignments = assignmentRepository.findAllReviewedAssignments(); // Ensure this method exists
        List<StudentDoubt> doubts = studentDoubtRepository.findAll();

        for (StudentDoubt d : doubts) {
            String userName = (d.getUser() != null && d.getUser().getFullName() != null)
                    ? d.getUser().getFullName()
                    : "Unknown";
            d.setDisplayTitle(d.getTitle() + " - " + userName);
        }
        model.addAttribute("video", new VideoSolution());
        model.addAttribute("assignments", assignments);
        model.addAttribute("doubts", doubts);
        model.addAttribute("videos", videos);
        return "tutor/tutor-video-solutions";
    }

    // ✅ Upload POST
    @PostMapping("/upload")
    public String uploadVideoSolution(@RequestParam("title") String title,
                                      @RequestParam("description") String description,
                                      @RequestParam(name = "assignmentId", required = false) Long assignmentId,
                                      @RequestParam(name = "doubtId", required = false) Long doubtId,
                                      @RequestParam("videoFile") MultipartFile videoFile,
                                      Principal principal) throws IOException {

        VideoSolution video = new VideoSolution();
        video.setTitle(title);
        video.setDescription(description);
        video.setCreatedAt(LocalDateTime.now());

        if (assignmentId != null) {
            assignmentRepository.findById(assignmentId).ifPresent(video::setAssignment);
        }

        if (doubtId != null) {
            studentDoubtRepository.findById(doubtId).ifPresent(video::setDoubt);
        }
        if (doubtId != null) {
            studentDoubtRepository.findById(doubtId).ifPresent(doubt -> {
                video.setDoubt(doubt);
                if (doubt.getUser() != null) {
                    video.setStudent(doubt.getUser()); // ✅ Set student from doubt's user
                }
            });
        }

        if (!videoFile.isEmpty()) {
            String filename = UUID.randomUUID() + "_" + StringUtils.cleanPath(videoFile.getOriginalFilename());
            File uploadPath = new File(videosUploadPath);
            if (!uploadPath.exists()) uploadPath.mkdirs();
            File destFile = new File(uploadPath, filename);
            videoFile.transferTo(destFile);
            video.setVideoPath(filename);
        }

        userRepository.findByEmail(principal.getName()).ifPresent(video::setUploadedBy);

        videoSolutionRepository.save(video);
        return "redirect:/tutor/videos";
    }

    @GetMapping("/tutor/videos")
    public String viewUploadedVideos(Model model, Principal principal) {
        User tutor = userRepository.findByEmail(principal.getName()).orElse(null);
        List<VideoSolution> videos = videoSolutionRepository.findByUploadedBy(tutor);
        model.addAttribute("videos", videos);
        return "tutor/tutor-video-list";
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
