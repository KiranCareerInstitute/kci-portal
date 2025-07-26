package com.kci.portal.controller;

import com.kci.portal.model.ChatMessage;
import com.kci.portal.model.User;
import com.kci.portal.repository.ChatMessageRepository;
import com.kci.portal.repository.UserRepository;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.*;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/tutor/session")
@RolesAllowed("TUTOR")
public class TutorSessionChatController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    // ✅ View full conversation with a student
    @GetMapping("/{studentEmail}")
    public String openChatWithStudent(@PathVariable String studentEmail,
                                      Model model,
                                      Principal principal) {
        String tutorEmail = principal.getName();

        List<ChatMessage> sent = chatMessageRepository
                .findBySenderEmailAndReceiverEmailOrderBySentAtAsc(tutorEmail, studentEmail);

        List<ChatMessage> received = chatMessageRepository
                .findBySenderEmailAndReceiverEmailOrderBySentAtAsc(studentEmail, tutorEmail);

        List<ChatMessage> fullChat = new ArrayList<>();
        fullChat.addAll(sent);
        fullChat.addAll(received);
        fullChat.sort(Comparator.comparing(ChatMessage::getSentAt));

        model.addAttribute("messages", fullChat);
        model.addAttribute("studentEmail", studentEmail);
        model.addAttribute("tutorEmail", tutorEmail);
        return "tutor/chat-room";
    }

    // ✅ Send message to student (text + optional file)
    @PostMapping("/send")
    public String sendReplyToStudent(@RequestParam String receiverEmail,
                                     @RequestParam String message,
                                     @RequestParam(name = "file", required = false) MultipartFile file,
                                     Principal principal,
                                     RedirectAttributes redirectAttributes) {

        String senderEmail = principal.getName();
        User tutor = userRepository.findByEmail(senderEmail).orElseThrow();
        User student = userRepository.findByEmail(receiverEmail).orElseThrow();

        // Check if student is restricted from chat
        if (student.isRestrictedFromChat()) {
            redirectAttributes.addFlashAttribute("error", "❌ This student is restricted. You cannot reply.");
            return "redirect:/tutor/session/" + receiverEmail;
        }

        ChatMessage chat = new ChatMessage();
        chat.setSenderEmail(senderEmail);
        chat.setReceiverEmail(receiverEmail);
        chat.setMessage(message);
        chat.setSentAt(LocalDateTime.now());
        chat.setStatus("REPLIED");

        // ✅ Handle file attachment if any
        if (file != null && !file.isEmpty()) {
            try {
                String originalName = file.getOriginalFilename();
                String ext = originalName.substring(originalName.lastIndexOf("."));
                String uniqueName = UUID.randomUUID() + ext;
                Path uploadPath = Paths.get("uploads/chat/" + uniqueName);
                Files.createDirectories(uploadPath.getParent());
                file.transferTo(uploadPath.toFile());

                chat.setFileName(originalName);
                chat.setFilePath(uniqueName);
            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("error", "❌ File upload failed.");
                return "redirect:/tutor/session/" + receiverEmail;
            }
        }

        chatMessageRepository.save(chat);
        redirectAttributes.addFlashAttribute("success", "✅ Reply sent.");
        return "redirect:/tutor/session/" + receiverEmail;
    }
    @GetMapping("/tutor/session/{studentEmail}")
    public String showChatWithStudent(@PathVariable String studentEmail, Principal principal, Model model) {
        String tutorEmail = principal.getName();

        // ✅ Get chat messages between tutor and student
        List<ChatMessage> messages = chatMessageRepository.findBySenderEmailAndReceiverEmailOrderBySentAtAsc(studentEmail, tutorEmail);
        messages.addAll(chatMessageRepository.findBySenderEmailAndReceiverEmailOrderBySentAtAsc(tutorEmail, studentEmail));

        // ✅ Sort messages by sentAt manually since we combined two lists
        messages.sort((m1, m2) -> m1.getSentAt().compareTo(m2.getSentAt()));

        model.addAttribute("messages", messages);
        model.addAttribute("studentEmail", studentEmail);
        model.addAttribute("tutorEmail", tutorEmail);

        return "tutor/tutor-chat-session";
    }
}
