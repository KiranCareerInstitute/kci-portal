package com.kci.portal.repository;

import com.kci.portal.model.SessionFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionFeedbackRepository extends JpaRepository<SessionFeedback, Long> {

    List<SessionFeedback> findBySessionSlotId(Long sessionSlotId);

    List<SessionFeedback> findBySessionSlotIdAndSubmittedBy(Long slotId, String submittedBy);

}
