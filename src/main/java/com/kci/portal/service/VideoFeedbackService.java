package com.kci.portal.service;

import com.kci.portal.model.User;
import com.kci.portal.model.VideoFeedback;
import com.kci.portal.model.VideoSolution;
import com.kci.portal.repository.VideoFeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VideoFeedbackService {

    @Autowired
    private VideoFeedbackRepository videoFeedbackRepository;

    public VideoFeedback saveFeedback(VideoFeedback feedback) {
        return videoFeedbackRepository.save(feedback);
    }

    public List<VideoFeedback> getFeedbackByVideo(VideoSolution videoSolution) {
        return videoFeedbackRepository.findByVideoSolution(videoSolution);
    }

    public List<VideoFeedback> getFeedbackByStudent(User student) {
        return videoFeedbackRepository.findByStudent(student);
    }

    public VideoFeedback findById(Long id) {
        return videoFeedbackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feedback not found with id: " + id));
    }

    public Optional<VideoFeedback> findByVideoSolutionAndStudent(VideoSolution video, User student) {
        List<VideoFeedback> feedbackList = videoFeedbackRepository.findAllByVideoSolutionAndStudent(video, student);
        return feedbackList.stream().findFirst(); // ✅ Safe: returns the first if multiple exist
    }
}
