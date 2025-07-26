package com.kci.portal.repository;

import com.kci.portal.model.StudentResult;
import com.kci.portal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudentResultRepository extends JpaRepository<StudentResult, Long> {

    // ✅ Get all results of a given student
    List<StudentResult> findByUser(User user);

    // ✅ Optional: Get results sorted by date
    List<StudentResult> findByUserOrderByDateTakenDesc(User user);
}
