package com.kci.portal.repository;

import com.kci.portal.model.SessionSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SessionSlotRepository extends JpaRepository<SessionSlot, Long> {

    // Get all unbooked slots (for students to view)
    List<SessionSlot> findByBookedFalseOrderByDateAscStartTimeAsc();

    // Get all slots booked by a specific student
    List<SessionSlot> findByStudentEmailOrderByDateAscStartTimeAsc(String studentEmail);

    // Get all slots created by a specific tutor (used for tutor's view)
    List<SessionSlot> findByTutorEmailOrderByDateAscStartTimeAsc(String tutorEmail);
}
