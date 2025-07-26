package com.kci.portal.controller;

import com.kci.portal.model.User;
import com.kci.portal.model.VideoFeedback;
import com.kci.portal.model.VideoSolution;
import com.kci.portal.repository.UserRepository;
import com.kci.portal.repository.VideoSolutionRepository;
import com.kci.portal.service.VideoFeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Optional;

@Controller
@RequestMapping("/video-feedback")
public class VideoFeedbackController {

    @Autowired
    private VideoFeedbackService videoFeedbackService;

    @Autowired
    private VideoSolutionRepository videoSolutionRepository;

    @Autowired
    private UserRepository userRepository;

    // Show feedback form
    @GetMapping("/submit/{videoId}")
    public String showFeedbackForm(@PathVariable Long videoId, Model model) {
        VideoSolution video = videoSolutionRepository.findById(videoId).orElseThrow();
        model.addAttribute("video", video);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User student = userRepository.findByEmail(email).orElseThrow();

        Optional<VideoFeedback> existing = videoFeedbackService
                .findByVideoSolutionAndStudent(video, student);

        if (existing.isPresent()) {
            model.addAttribute("feedbackSubmitted", true);
            model.addAttribute("feedback", existing.get());
        } else {
            model.addAttribute("feedbackSubmitted", false);
            model.addAttribute("feedback", new VideoFeedback());
        }

        return "student/video_feedback_form";
    }


    @PostMapping("/submit/{videoId}")
    public String submitFeedback(@PathVariable Long videoId,
                                 @ModelAttribute("feedback") VideoFeedback feedback,
                                 RedirectAttributes redirectAttributes) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User student = userRepository.findByEmail(email).orElseThrow();
        VideoSolution video = videoSolutionRepository.findById(videoId).orElseThrow();

        Optional<VideoFeedback> existing = videoFeedbackService.findByVideoSolutionAndStudent(video, student);

        if (existing.isPresent()) {
            // Update existing feedback
            VideoFeedback existingFeedback = existing.get();
            existingFeedback.setRating(feedback.getRating());
            existingFeedback.setComment(feedback.getComment());
            videoFeedbackService.saveFeedback(existingFeedback);
        } else {
            feedback.setStudent(student);
            feedback.setVideoSolution(video);
            videoFeedbackService.saveFeedback(feedback);
        }

        redirectAttributes.addFlashAttribute("successMessage", "✅ Feedback submitted successfully!");

        return "redirect:/video-feedback/submit/" + videoId;
    }
}
