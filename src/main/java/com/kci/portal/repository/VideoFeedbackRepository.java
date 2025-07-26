package com.kci.portal.repository;

import com.kci.portal.model.User;
import com.kci.portal.model.VideoFeedback;
import com.kci.portal.model.VideoSolution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VideoFeedbackRepository extends JpaRepository<VideoFeedback, Long> {

    List<VideoFeedback> findByVideoSolution(VideoSolution videoSolution);

    List<VideoFeedback> findByStudent(User student);

    @Query("SELECT f FROM VideoFeedback f WHERE f.videoSolution = :video AND f.student = :student")
    List<VideoFeedback> findAllByVideoSolutionAndStudent(@Param("video") VideoSolution video,
                                                         @Param("student") User student);
}
