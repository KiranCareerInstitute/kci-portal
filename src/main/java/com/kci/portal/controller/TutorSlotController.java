package com.kci.portal.controller;

import com.kci.portal.model.SessionSlot;
import com.kci.portal.service.SessionSlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/tutor")
@PreAuthorize("hasRole('TUTOR')")
public class TutorSlotController {

    @Autowired
    private SessionSlotService slotService;

    // ✅ Show form to add a slot
    @GetMapping("/availability")
    public String showAvailabilityForm(Model model) {
        model.addAttribute("slot", new SessionSlot());
        return "tutor/add-availability";
    }

    // ✅ Save new availability
    @PostMapping("/availability")
    public String saveAvailability(@ModelAttribute("slot") SessionSlot slot,
                                   Principal principal) {
        slot.setTutorEmail(principal.getName());
        slot.setBooked(false); // new slots are unbooked
        slotService.saveSlot(slot);
        return "redirect:/tutor/availability?success";
    }

    // ✅ View all slots by tutor
    @GetMapping("/my-slots")
    public String viewMySlots(Model model, Principal principal) {
        List<SessionSlot> mySlots = slotService.getSlotsByTutor(principal.getName());
        model.addAttribute("mySlots", mySlots);
        return "tutor/my-slots";
    }
}
