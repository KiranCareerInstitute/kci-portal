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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/videos")
public class AdminVideoController {

    @Autowired private VideoSolutionRepository videoSolutionRepository;
    @Autowired private AssignmentRepository     assignmentRepository;
    @Autowired private UserRepository           userRepository;
    @Autowired private VideoFeedbackRepository  videoFeedbackRepository;
    @Autowired private StudentDoubtRepository   studentDoubtRepository;

    /** physical folder where videos are stored */
    @Value("${upload.path.videos}")
    private String videosUploadPath;

    @Value("${upload.path.tests}")
    private String testUploadPath;

    /** URL path your ResourceHandler will serve from that folder */
    private static final String UPLOAD_URL_PREFIX = "/uploads/videos";

    @GetMapping
    public String showFilteredOrAllVideos(
            @RequestParam(required = false) String uploaderType,
            @RequestParam(required = false) Long   assignmentId,
            Model model) {

        List<VideoSolution> videos = (uploaderType!=null||assignmentId!=null)
                ? videoSolutionRepository.findFiltered(uploaderType, assignmentId)
                : videoSolutionRepository.findAll();

        model.addAttribute("videos",              videos);
        model.addAttribute("assignments",         assignmentRepository.findAll());
        model.addAttribute("doubts",              studentDoubtRepository.findAll());
        model.addAttribute("uploaderType",        uploaderType);
        model.addAttribute("selectedAssignmentId",assignmentId);
        model.addAttribute("uploadUrlPrefix",     UPLOAD_URL_PREFIX);
        return "admin/admin-video-solutions";
    }

    @PostMapping("/upload")
    public String uploadVideoSolution(
            @RequestParam("title")       String        title,
            @RequestParam("description") String        description,
            @RequestParam(name="assignmentId", required=false) Long  assignmentId,
            @RequestParam(name="doubtId",      required=false) Long  doubtId,
            @RequestParam("videoFile") MultipartFile        videoFile,
            Principal                           principal
    ) throws IOException {

        VideoSolution vs = new VideoSolution();
        vs.setTitle(title);
        vs.setDescription(description);
        vs.setCreatedAt(LocalDateTime.now());

        // who uploaded it
        userRepository.findByEmail(principal.getName())
                .ifPresent(vs::setUploadedBy);

        // link assignment & doubt
        if (assignmentId != null)
            assignmentRepository.findById(assignmentId)
                    .ifPresent(vs::setAssignment);

        if (doubtId != null)
            studentDoubtRepository.findById(doubtId).ifPresent(d -> {
                vs.setDoubt(d);
                if (d.getUser() != null)
                    vs.setStudent(d.getUser());
            });

        // ─── handle file storage ──────────────────────────────────────────
        if (!videoFile.isEmpty()) {
            // ensure folder exists
            Path uploadDir = Paths.get(videosUploadPath)
                    .toAbsolutePath()
                    .normalize();
            Files.createDirectories(uploadDir);

            // build safe, unique filename
            String originalName = StringUtils.cleanPath(videoFile.getOriginalFilename());
            String uniqueName   = System.currentTimeMillis()
                    + "_" + UUID.randomUUID()
                    + "_" + originalName;

            // copy to disk
            Path target = uploadDir.resolve(uniqueName);
            videoFile.transferTo(target.toFile());

            vs.setVideoPath(uniqueName);
        }
        // ────────────────────────────────────────────────────────────────

        videoSolutionRepository.save(vs);
        return "redirect:/admin/videos";
    }

    @PostMapping("/delete/{id}")
    public String deleteVideo(@PathVariable Long id) {
        videoSolutionRepository.deleteById(id);
        return "redirect:/admin/videos";
    }

    @GetMapping("/video-feedback/{videoId}")
    public String viewFeedbackForVideo(@PathVariable Long videoId, Model model) {
        VideoSolution video = videoSolutionRepository.findById(videoId)
                .orElseThrow();
        model.addAttribute("video",     video);
        model.addAttribute("feedbacks", video.getFeedbackList());
        model.addAttribute("uploadUrlPrefix", UPLOAD_URL_PREFIX);
        return "admin/video_feedback_detail";
    }

    @PostMapping("/feedback/respond/{feedbackId}")
    @Transactional
    public String submitAdminResponse(@PathVariable Long feedbackId,
                                      @RequestParam String adminResponse) {
        videoFeedbackRepository.findById(feedbackId)
                .ifPresent(f -> {
                    f.setAdminResponse(adminResponse);
                    // no need to call save() because of @Transactional
                });
        Long vid = videoFeedbackRepository.findById(feedbackId)
                .get()
                .getVideoSolution()
                .getId();
        return "redirect:/admin/videos/video-feedback/" + vid;
    }
}
