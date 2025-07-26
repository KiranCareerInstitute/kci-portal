package com.kci.portal.repository;

import com.kci.portal.model.Assignment;
import com.kci.portal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    // ✅ Student: Get all assignments submitted by a specific user
    List<Assignment> findByUser(User user);

    // ✅ Admin: Search assignments by student name or file path
    List<Assignment> findByUserFullNameContainingIgnoreCaseOrFilePathContainingIgnoreCase(String fullName, String filePath);

    // ✅ Admin: Count by status (e.g., Pending/Reviewed)
    long countByStatus(String status);

    // ✅ Admin: Load all assignments with joined user (no lazy loading)
    @Query("SELECT a FROM Assignment a LEFT JOIN FETCH a.user")
    List<Assignment> findAllWithUser();

    @Query("SELECT a FROM Assignment a WHERE a.status = 'REVIEWED'")
    List<Assignment> findAllReviewed();

    @Query("SELECT a FROM Assignment a WHERE a.status = 'REVIEWED'")
    List<Assignment> findAllReviewedAssignments();
}
