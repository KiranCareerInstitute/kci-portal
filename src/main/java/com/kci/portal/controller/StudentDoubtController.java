package com.kci.portal.controller;

import com.kci.portal.model.StudentDoubt;
import com.kci.portal.repository.StudentDoubtRepository;
import com.kci.portal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    /** matches upload.path.doubts in application.properties */
    @Value("${upload.path.doubts}")
    private String doubtUploadDir;

    @GetMapping("/upload")
    public String showUploadForm(Model model) {
        model.addAttribute("doubt", new StudentDoubt());
        return "student/student-upload-doubt";
    }

    @PostMapping("/upload")
    public String uploadDoubt(
            @ModelAttribute StudentDoubt doubt,
            @RequestParam("file") MultipartFile file,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a file to upload.");
            return "redirect:/student/doubts/upload";
        }

        try {
            // 1) Resolve and create upload dir
            Path uploadPath = Paths.get(doubtUploadDir)
                    .toAbsolutePath()
                    .normalize();
            Files.createDirectories(uploadPath);

            // 2) Generate safe filename
            String originalName = StringUtils.cleanPath(
                    file.getOriginalFilename()
            );
            String filename = System.currentTimeMillis()
                    + "_" + UUID.randomUUID()
                    + "_" + originalName;

            // 3) Copy file to target
            Path targetFile = uploadPath.resolve(filename);
            file.transferTo(targetFile.toFile());

            // 4) Persist metadata
            doubt.setStudentEmail(principal.getName());
            doubt.setFileName(originalName);
            doubt.setFilePath(filename);
            doubt.setSubmittedAt(LocalDateTime.now());
            doubt.setStatus("PENDING");
            doubtRepository.save(doubt);

            redirectAttributes.addFlashAttribute("success", "✅ Doubt submitted successfully!");
        } catch (IOException e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute(
                    "error",
                    "❌ Failed to upload file: " + e.getMessage()
            );
        }

        return "redirect:/student/doubts/upload";
    }

    @GetMapping("/my")
    public String viewMyDoubts(Model model, Principal principal) {
        model.addAttribute(
                "doubts",
                doubtRepository.findByStudentEmailOrderBySubmittedAtDesc(principal.getName())
        );
        return "student/student-my-doubts";
    }
}