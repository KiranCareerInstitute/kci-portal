package com.kci.portal.controller;

import com.kci.portal.model.SessionSlot;
import com.kci.portal.model.User;
import com.kci.portal.repository.UserRepository;
import com.kci.portal.service.SessionSlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@PreAuthorize("hasRole('USER')")
public class StudentSessionController {

    @Autowired
    private SessionSlotService slotService;

    @Autowired
    private UserRepository userRepository;

    // ✅ Show available and booked slots to student
    @GetMapping("/student/book-session")
    public String viewSlots(Model model, Principal principal) {
        String studentEmail = principal.getName();

        List<SessionSlot> availableSlots = slotService.getAvailableSlots();
        List<SessionSlot> bookedSlots = slotService.getBookedSlotsByStudent(studentEmail);
        User user = userRepository.findByEmail(studentEmail).orElseThrow();

        model.addAttribute("slots", availableSlots);
        model.addAttribute("bookedSlots", bookedSlots);
        model.addAttribute("user", user);

        return "student/student-book-session";
    }

    // ✅ Book a slot
    @PostMapping("/student/book-session/{id}")
    public String bookSlot(@PathVariable Long id,
                           Principal principal,
                           RedirectAttributes redirectAttributes) {
        String studentEmail = principal.getName();

        try {
            SessionSlot slot = slotService.getSlotById(id);
            if (slot == null || slot.isBooked()) {
                redirectAttributes.addFlashAttribute("error", "❌ Slot is no longer available.");
                return "redirect:/student/book-session";
            }

            slot.setBooked(true);
            slot.setStudentEmail(studentEmail);
            slotService.saveSlot(slot);

            redirectAttributes.addFlashAttribute("success", "✅ Session booked successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "❌ Booking failed due to system error.");
        }

        return "redirect:/student/book-session?booked";
    }
}
