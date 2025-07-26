package com.kci.portal.repository;

import com.kci.portal.model.VideoSolution;
import com.kci.portal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VideoSolutionRepository extends JpaRepository<VideoSolution, Long> {

    // Get all videos for a particular student
    List<VideoSolution> findByStudentEmail(String email);

    // Get all videos for a particular assignment
    List<VideoSolution> findByAssignmentId(Long assignmentId);

    // Get all videos uploaded by a specific user
    List<VideoSolution> findByUploadedBy(User uploadedBy);

    // Filtered search for admin video dashboard using uploaderType and assignmentId
    @Query("""
        SELECT v FROM VideoSolution v
        JOIN v.uploadedBy u
        JOIN UserRole ur ON ur.user = u
        WHERE (:uploaderType IS NULL OR :uploaderType = '' OR ur.roles LIKE CONCAT('%', :uploaderType))
        AND (:assignmentId IS NULL OR v.assignment.id = :assignmentId)
    """)
    List<VideoSolution> findFiltered(@Param("uploaderType") String uploaderType,
                                     @Param("assignmentId") Long assignmentId);
}
