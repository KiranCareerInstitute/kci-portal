package com.kci.portal.repository;

import com.kci.portal.model.TestSubmission;
import com.kci.portal.model.User;
import com.kci.portal.model.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface TestSubmissionRepository extends JpaRepository<TestSubmission, Long> {
    List<TestSubmission> findByUser(User user);
    List<TestSubmission> findByTest(Test test);

    // New methods
    List<TestSubmission> findByMarksIsNull();       // Pending review
    List<TestSubmission> findByMarksIsNotNull();    // Reviewed

    @Query("SELECT COUNT(ts) FROM TestSubmission ts")
    long countAllSubmissions();

    @Query("SELECT COUNT(ts) FROM TestSubmission ts WHERE ts.marks IS NULL")
    long countPendingSubmissions();

    @Query("SELECT COUNT(ts) FROM TestSubmission ts WHERE ts.marks IS NOT NULL")
    long countReviewedSubmissions();

    @Query("SELECT s FROM TestSubmission s JOIN FETCH s.user u JOIN FETCH s.test t WHERE " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :studentName, '%')) OR " +
            "LOWER(t.title) LIKE LOWER(CONCAT('%', :testTitle, '%'))")
    List<TestSubmission> findByUserFullNameContainingIgnoreCaseOrTestTitleContainingIgnoreCase(
            @Param("studentName") String studentName,
            @Param("testTitle") String testTitle
    );
    // You can keep your existing findAllWithUserAndTest() if it exists, or define it like:
    @Query("SELECT s FROM TestSubmission s JOIN FETCH s.user u JOIN FETCH s.test t")
    List<TestSubmission> findAllWithUserAndTest();

        @Query("SELECT ts FROM TestSubmission ts " +
            "WHERE (:keyword IS NULL OR LOWER(ts.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "   OR LOWER(ts.test.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR " +
            "     (:status = 'pending' AND ts.marks IS NULL) OR " +
            "     (:status = 'reviewed' AND ts.marks IS NOT NULL))")
    Page<TestSubmission> searchSubmissions(
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable
    );
}
