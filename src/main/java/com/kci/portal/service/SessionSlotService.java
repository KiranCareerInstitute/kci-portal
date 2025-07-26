package com.kci.portal.service;

import com.kci.portal.model.SessionSlot;
import com.kci.portal.repository.SessionSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SessionSlotService {

    @Autowired
    private SessionSlotRepository slotRepository;

    // 🟢 Get all available (unbooked) slots
    public List<SessionSlot> getAvailableSlots() {
        return slotRepository.findByBookedFalseOrderByDateAscStartTimeAsc();
    }

    // 🟢 Get all slots booked by a specific student
    public List<SessionSlot> getBookedSlotsByStudent(String studentEmail) {
        return slotRepository.findByStudentEmailOrderByDateAscStartTimeAsc(studentEmail);
    }

    // 🟢 Get all slots created by a specific tutor
    public List<SessionSlot> getSlotsByTutor(String tutorEmail) {
        return slotRepository.findByTutorEmailOrderByDateAscStartTimeAsc(tutorEmail);
    }

    // 🟢 Get a single slot by ID
    public SessionSlot getSlotById(Long id) {
        return slotRepository.findById(id).orElse(null);
    }

    // 🟢 Save or update a slot
    public void saveSlot(SessionSlot slot) {
        slotRepository.save(slot);
    }
}
