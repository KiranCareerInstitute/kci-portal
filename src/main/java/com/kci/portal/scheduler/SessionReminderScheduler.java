package com.kci.portal.scheduler;

import com.kci.portal.model.SessionSlot;
import com.kci.portal.repository.SessionSlotRepository;
import com.kci.portal.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class SessionReminderScheduler {

    @Autowired
    private SessionSlotRepository slotRepository;

    @Autowired
    private EmailService emailService;

    // 🔁 Runs every 5 minutes
    @Scheduled(fixedRate = 300000)
    public void sendSessionReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime target = now.plusMinutes(30); // ⏰ Upcoming 30 minutes

        List<SessionSlot> upcomingSessions = slotRepository.findAll().stream()
                .filter(SessionSlot::isBooked)
                .filter(slot -> {
                    LocalDateTime sessionStart = slot.getDate().atTime(slot.getStartTime());
                    return sessionStart.isAfter(now) && sessionStart.isBefore(target);
                })
                .toList();

        for (SessionSlot slot : upcomingSessions) {
            String tutorEmail = slot.getTutorEmail();
            String studentEmail = slot.getStudentEmail();

            String sessionTime = slot.getDate() + " at " + slot.getStartTime();

            // 📧 Notify Tutor
            emailService.sendReminder(
                    tutorEmail,
                    "Upcoming Session Reminder",
                    "You have a session with " + studentEmail + " scheduled at " + sessionTime + ".");

            // 📧 Notify Student
            emailService.sendReminder(
                    studentEmail,
                    "Your Tutoring Session Reminder",
                    "Your session with " + tutorEmail + " is scheduled at " + sessionTime + ".");
        }
    }
}
