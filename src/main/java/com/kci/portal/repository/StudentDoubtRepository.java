package com.kci.portal.repository;

import com.kci.portal.model.StudentDoubt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudentDoubtRepository extends JpaRepository<StudentDoubt, Long> {

    // For students
    List<StudentDoubt> findByStudentEmailOrderBySubmittedAtDesc(String email);

    // For admin
    List<StudentDoubt> findAllByOrderBySubmittedAtDesc();

    // ✅ NEW: For tutors - only assigned doubts
    List<StudentDoubt> findByAssignedTutor_EmailOrderBySubmittedAtDesc(String email);

    List<StudentDoubt> findByStatusOrderBySubmittedAtDesc(String status);
    List<StudentDoubt> findByAssignedTutorIsNullOrderBySubmittedAtDesc();
    List<StudentDoubt> findByAssignedTutorIsNotNullOrderBySubmittedAtDesc();
    List<StudentDoubt> findByStatusAndAssignedTutorIsNullOrderBySubmittedAtDesc(String status);
    List<StudentDoubt> findByStatusAndAssignedTutorIsNotNullOrderBySubmittedAtDesc(String status);
    List<StudentDoubt> findByAssignedTutorEmailOrderBySubmittedAtDesc(String email);

}
