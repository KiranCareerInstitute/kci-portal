package com.kci.portal.controller;

import com.kci.portal.model.Assignment;
import com.kci.portal.model.StudentDoubt;
import com.kci.portal.model.User;
import com.kci.portal.model.VideoSolution;
import com.kci.portal.repository.AssignmentRepository;
import com.kci.portal.repository.StudentDoubtRepository;
import com.kci.portal.repository.UserRepository;
import com.kci.portal.repository.VideoFeedbackRepository;
import com.kci.portal.repository.VideoSolutionRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.kci.portal.model.VideoFeedback;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/videos")
public class AdminVideoController {

    @Autowired
    private VideoSolutionRepository videoSolutionRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoFeedbackRepository videoFeedbackRepository;

    @Autowired
    private StudentDoubtRepository studentDoubtRepository;

    @Value("${upload.path.videos}")
    private String videosUploadPath;

    @GetMapping
    public String showFilteredOrAllVideos(
            @RequestParam(required = false) String uploaderType,
            @RequestParam(required = false) Long assignmentId,
            Model model) {

        List<VideoSolution> videos;
        if ((uploaderType != null && !uploaderType.isEmpty()) || assignmentId != null) {
            videos = videoSolutionRepository.findFiltered(uploaderType, assignmentId);
        } else {
            videos = videoSolutionRepository.findAll();
        }

        List<Assignment> assignments = assignmentRepository.findAll();
        List<StudentDoubt> doubts = studentDoubtRepository.findAll();

        model.addAttribute("videos", videos);
        model.addAttribute("assignments", assignments);
        model.addAttribute("doubts", doubts);
        model.addAttribute("uploaderType", uploaderType);
        model.addAttribute("selectedAssignmentId", assignmentId);

        return "admin/admin-video-solutions";
    }

    @PostMapping("/upload")
    public String uploadVideoSolution(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam(name = "assignmentId", required = false) Long assignmentId,
            @RequestParam(name = "doubtId", required = false) Long doubtId,
            @RequestParam("videoFile") MultipartFile videoFile,
            Principal principal) throws IOException {

        VideoSolution videoSolution = new VideoSolution();
        videoSolution.setTitle(title);
        videoSolution.setDescription(description);
        videoSolution.setCreatedAt(LocalDateTime.now());

        // Use actual logged-in user as uploader
        userRepository.findByEmail(principal.getName()).ifPresent(videoSolution::setUploadedBy);

        // Link assignment if present
        if (assignmentId != null) {
            assignmentRepository.findById(assignmentId).ifPresent(videoSolution::setAssignment);
        }

        // Link doubt if present
        if (doubtId != null) {
            studentDoubtRepository.findById(doubtId).ifPresent(doubt -> {
                videoSolution.setDoubt(doubt);
                if (doubt.getUser() != null) {
                    videoSolution.setStudent(doubt.getUser());
                }
            });
        }

        // Handle file
        if (!videoFile.isEmpty()) {
            String filename = UUID.randomUUID() + "_" + StringUtils.cleanPath(videoFile.getOriginalFilename());
            File uploadPath = new File(videosUploadPath);
            if (!uploadPath.exists()) uploadPath.mkdirs();

            File destFile = new File(uploadPath, filename);
            videoFile.transferTo(destFile);
            videoSolution.setVideoPath(filename);
        }

        videoSolutionRepository.save(videoSolution);
        return "redirect:/admin/videos";
    }

    @PostMapping("/delete/{id}")
    public String deleteVideo(@PathVariable Long id) {
        videoSolutionRepository.deleteById(id);
        return "redirect:/admin/videos";
    }

    @GetMapping("/video-feedback/{videoId}")
    public String viewFeedbackForVideo(@PathVariable Long videoId, Model model) {
        VideoSolution video = videoSolutionRepository.findById(videoId).orElseThrow();
        model.addAttribute("video", video);
        model.addAttribute("feedbacks", video.getFeedbackList());
        return "admin/video_feedback_detail";
    }

    @PostMapping("/feedback/respond/{feedbackId}")
    @Transactional
    public String submitAdminResponse(@PathVariable Long feedbackId,
                                      @RequestParam String adminResponse) {
        VideoFeedback feedback = videoFeedbackRepository.findById(feedbackId).orElseThrow();
        feedback.setAdminResponse(adminResponse);
        return "redirect:/admin/videos/video-feedback/" + feedback.getVideoSolution().getId();
    }
}
