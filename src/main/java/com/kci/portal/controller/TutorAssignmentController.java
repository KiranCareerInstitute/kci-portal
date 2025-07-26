package com.kci.portal.controller.tutor;

import com.kci.portal.model.Assignment;
import com.kci.portal.repository.AssignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class TutorAssignmentController {

    @Autowired
    private AssignmentRepository assignmentRepository;

    @GetMapping("/tutor/assignments")
    public String viewReviewedAssignments(Model model) {
        List<Assignment> reviewedAssignments = assignmentRepository.findAllReviewedAssignments();
        model.addAttribute("assignments", reviewedAssignments);
        return "tutor/tutor-assignments";  // ✅ View to be created in next step
    }
}
