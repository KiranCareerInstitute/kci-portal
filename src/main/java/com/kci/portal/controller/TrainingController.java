package com.kci.portal.controller;

import com.kci.portal.repository.TrainingModuleRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TrainingController {

    private final TrainingModuleRepository trainingModuleRepository;

    public TrainingController(TrainingModuleRepository trainingModuleRepository) {
        this.trainingModuleRepository = trainingModuleRepository;
    }

    @GetMapping("/training")
    public String showTrainingModules(Model model) {
        model.addAttribute("modules", trainingModuleRepository.findAll());
        return "training-modules";
    }
}
