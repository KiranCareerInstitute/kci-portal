package com.kci.portal.controller;

import com.kci.portal.model.User;
import com.kci.portal.model.VideoSolution;
import com.kci.portal.repository.UserRepository;
import com.kci.portal.repository.VideoSolutionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/student/videos")
public class StudentVideoController {

    @Autowired
    private VideoSolutionRepository videoSolutionRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public String viewVideoSolutions(Model model, Principal principal) {
        User student = userRepository.findByEmail(principal.getName()).orElse(null);
        List<VideoSolution> videos = videoSolutionRepository.findAll().stream()
                .filter(v -> v.getStudent() == null
                        || (student != null && v.getStudent().getId().equals(student.getId())))
                .toList();
        model.addAttribute("videos", videos);
        return "student-view-videos";  // this must match student-view-videos.html
    }
}
