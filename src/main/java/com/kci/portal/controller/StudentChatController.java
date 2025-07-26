package com.kci.portal.controller;

import com.kci.portal.model.ChatMessage;
import com.kci.portal.model.User;
import com.kci.portal.repository.ChatMessageRepository;
import com.kci.portal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/student/chat")
public class StudentChatController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    // ✅ Show chat form
    @GetMapping
    public String showChatForm(Model model, Principal principal) {
        List<User> tutors = userRepository.findByRole("ROLE_TUTOR");
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        model.addAttribute("tutors", tutors);
        model.addAttribute("user", user);
        return "student/student-send-message";
    }

    // ✅ Send message to tutor — restriction check from User
    @PostMapping("/send")
    public String sendChatMessage(
            @RequestParam String receiverEmail,
            @RequestParam String message,
            @RequestParam(name = "file", required = false) MultipartFile file,
            Principal principal,
            Model model) {

        String senderEmail = principal.getName();
        User student = userRepository.findByEmail(senderEmail).orElseThrow();

        // Restriction check
        if (student.isRestrictedFromChat()) {
            model.addAttribute("error", "❌ You are restricted from sending messages.");
            // Return form view with error
            List<User> tutors = userRepository.findByRole("ROLE_TUTOR");
            model.addAttribute("tutors", tutors);
            model.addAttribute("user", student);
            return "student/student-send-message";
        }

        ChatMessage chat = new ChatMessage();
        chat.setSenderEmail(senderEmail);
        chat.setReceiverEmail(receiverEmail);
        chat.setMessage(message);
        chat.setSentAt(LocalDateTime.now());
        chat.setStatus("UNREAD");

        // Save file if any
        if (file != null && !file.isEmpty()) {
            try {
                String originalFileName = file.getOriginalFilename();
                String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
                String uniqueFileName = UUID.randomUUID() + extension;
                Path uploadPath = Paths.get("uploads/chat/" + uniqueFileName);

                Files.createDirectories(uploadPath.getParent());
                file.transferTo(uploadPath.toFile());

                chat.setFileName(originalFileName);
                chat.setFilePath(uniqueFileName);
            } catch (IOException e) {
                model.addAttribute("error", "❌ File upload failed.");
                List<User> tutors = userRepository.findByRole("ROLE_TUTOR");
                model.addAttribute("tutors", tutors);
                model.addAttribute("user", student);
                return "student/student-send-message";
            }
        }

        chatMessageRepository.save(chat);

        // Show form again with success message
        model.addAttribute("success", "✅ Message sent!");
        List<User> tutors = userRepository.findByRole("ROLE_TUTOR");
        model.addAttribute("tutors", tutors);
        model.addAttribute("user", student);

        return "student/student-send-message";
    }

    // ✅ View chat history
    @GetMapping("/history")
    public String viewChatHistory(@RequestParam(value = "keyword", required = false) String keyword,
                                  @RequestParam(value = "tutorEmail", required = false) String tutorEmail,
                                  @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                  @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                  Principal principal,
                                  Model model) {

        String studentEmail = principal.getName();
        User user = userRepository.findByEmail(studentEmail).orElseThrow();

        List<ChatMessage> messages = chatMessageRepository.findBySenderEmailOrderBySentAtDesc(studentEmail);
        messages = messages.stream().filter(msg ->
                (tutorEmail == null || tutorEmail.isBlank() || msg.getReceiverEmail().equalsIgnoreCase(tutorEmail)) &&
                        (keyword == null || keyword.isBlank() ||
                                msg.getMessage().toLowerCase().contains(keyword.toLowerCase()) ||
                                (msg.getReply() != null && msg.getReply().toLowerCase().contains(keyword.toLowerCase()))
                        ) &&
                        (fromDate == null || !msg.getSentAt().toLocalDate().isBefore(fromDate)) &&
                        (toDate == null || !msg.getSentAt().toLocalDate().isAfter(toDate))
        ).toList();

        List<String> allTutors = chatMessageRepository.findBySenderEmailOrderBySentAtDesc(studentEmail)
                .stream().map(ChatMessage::getReceiverEmail).distinct().toList();

        model.addAttribute("messages", messages);
        model.addAttribute("tutors", allTutors);
        model.addAttribute("selectedTutor", tutorEmail);
        model.addAttribute("keyword", keyword);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("user", user);

        return "student/student-chat-history";
    }

    // ✅ "Ask a Tutor" — also blocked if restricted
    @PostMapping("/ask")
    public String sendQuery(@RequestParam String message,
                            Principal principal,
                            RedirectAttributes redirectAttributes) {
        String email = principal.getName();
        User student = userRepository.findByEmail(email).orElseThrow();

        if (student.isRestrictedFromChat()) {
            redirectAttributes.addFlashAttribute("error", "❌ You are restricted from sending messages.");
            return "redirect:/student/chat";
        }

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSenderEmail(email);
        chatMessage.setReceiverEmail("tutor@kci.com");
        chatMessage.setMessage(message);
        chatMessage.setSentAt(LocalDateTime.now());
        chatMessage.setStatus("UNREAD");

        chatMessageRepository.save(chatMessage);
        redirectAttributes.addFlashAttribute("success", "✅ Message sent!");
        return "redirect:/student/chat";
    }
}
