package com.kci.portal.controller;

import com.kci.portal.model.StudentDoubt;
import com.kci.portal.repository.StudentDoubtRepository;
import com.kci.portal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.UUID;

@Controller
@RequestMapping("/student/doubts")
public class StudentDoubtController {

    @Autowired
    private StudentDoubtRepository doubtRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${upload.path.doubts}")
    private String doubtUploadPath;

    // ✅ GET: Doubt submission form
    @GetMapping("/upload")
    public String showUploadForm(Model model) {
        model.addAttribute("doubt", new StudentDoubt());
        return "student/student-upload-doubt";
    }

    // ✅ POST: Handle submission
    @PostMapping("/upload")
    public String uploadDoubt(@ModelAttribute StudentDoubt doubt,
                              @RequestParam("file") MultipartFile file,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        try {
            String studentEmail = principal.getName();
            String originalFilename = file.getOriginalFilename();
            String uniqueName = UUID.randomUUID() + "_" + originalFilename;
            File uploadDir = new File(doubtUploadPath + File.separator + "doubts");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            File destFile = new File(doubtUploadPath, uniqueName); // correct path
            file.transferTo(destFile);

            doubt.setStudentEmail(studentEmail);
            doubt.setFileName(originalFilename);
            doubt.setFilePath("doubts/" + uniqueName);
            doubt.setSubmittedAt(LocalDateTime.now());
            doubt.setStatus("PENDING");

            doubtRepository.save(doubt);
            redirectAttributes.addFlashAttribute("success", "✅ Doubt submitted successfully!");
        } catch (IOException e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "❌ Failed to upload file. Try again.");
        }
        return "redirect:/student/doubts/upload";
    }
    // ✅ Show all doubts submitted by the logged-in student
    @GetMapping("/my")
    public String viewMyDoubts(Model model, Principal principal) {
        String studentEmail = principal.getName();
        model.addAttribute("doubts", doubtRepository.findByStudentEmailOrderBySubmittedAtDesc(studentEmail));
        return "student/student-my-doubts";  // ✅ This HTML page must exist
    }

}
