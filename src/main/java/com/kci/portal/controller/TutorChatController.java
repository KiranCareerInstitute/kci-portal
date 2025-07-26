package com.kci.portal.controller;

import com.kci.portal.model.ChatMessage;
import com.kci.portal.model.User;
import com.kci.portal.repository.ChatMessageRepository;
import com.kci.portal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/tutor/chat")
@PreAuthorize("hasRole('TUTOR')")
public class TutorChatController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    // ✅ Show tutor inbox (incoming student messages without replies)
    @GetMapping
    public String viewChatHistoryForTutor(Model model, Principal principal) {
        String tutorEmail = principal.getName();
        List<ChatMessage> messages = chatMessageRepository
                .findByReceiverEmailAndReplyIsNullOrderBySentAtDesc(tutorEmail);
        model.addAttribute("messages", messages);

        // 🔒 Pass list of restricted student emails for badge display
        List<String> restrictedStudents = userRepository.findByRole("ROLE_USER").stream()
                .filter(User::isRestrictedFromChat)
                .map(User::getEmail)
                .toList();
        model.addAttribute("restrictedStudents", restrictedStudents);

        return "tutor/tutor-chat-inbox";
    }

    // ✅ View tutor’s replied chat history
    @GetMapping("/history")
    public String viewFullChatHistory(@RequestParam(value = "studentEmail", required = false) String studentEmail,
                                      Model model,
                                      Principal principal) {
        String tutorEmail = principal.getName();

        List<ChatMessage> messages = chatMessageRepository
                .findByReceiverEmailAndReplyIsNotNullOrderBySentAtDesc(tutorEmail);

        if (studentEmail != null && !studentEmail.isBlank()) {
            messages = messages.stream()
                    .filter(m -> m.getSenderEmail().equalsIgnoreCase(studentEmail))
                    .toList();
        }

        // Collect distinct student emails for filter dropdown
        List<String> studentEmails = messages.stream()
                .map(ChatMessage::getSenderEmail)
                .distinct()
                .toList();

        model.addAttribute("messages", messages);
        model.addAttribute("studentEmails", studentEmails);
        model.addAttribute("selectedStudent", studentEmail);

        return "tutor/tutor-chat-history";
    }

    // ✅ Handle tutor reply with restriction check
    @PostMapping("/reply/{id}")
    public String replyToMessage(@PathVariable Long id,
                                 @RequestParam String reply,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        try {
            ChatMessage original = chatMessageRepository.findById(id).orElseThrow();

            // 🔒 Check if the student is restricted
            User student = userRepository.findByEmail(original.getSenderEmail()).orElseThrow();
            if (student.isRestrictedFromChat()) {
                redirectAttributes.addFlashAttribute("error", "❌ This student is restricted. You cannot reply.");
                return "redirect:/tutor/chat";
            }

            // ✅ Save reply in original message
            original.setReply(reply);
            original.setRepliedAt(LocalDateTime.now());
            original.setStatus("REPLIED");
            chatMessageRepository.save(original);

            // ✅ Save reply as new message (visible to student)
            ChatMessage replyMsg = new ChatMessage();
            replyMsg.setSenderEmail(principal.getName()); // tutor
            replyMsg.setReceiverEmail(original.getSenderEmail()); // student
            replyMsg.setMessage(reply);
            replyMsg.setSentAt(LocalDateTime.now());
            replyMsg.setStatus("REPLIED");

            chatMessageRepository.save(replyMsg);

            redirectAttributes.addFlashAttribute("success", "✅ Reply sent successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "❌ Reply failed due to system error.");
        }

        return "redirect:/tutor/chat";
    }

    // ✅ Count unread student messages (used in layout)
    @ModelAttribute("newMessageCount")
    public int getNewMessages(Principal principal) {
        String tutorEmail = principal.getName();
        return (int) chatMessageRepository.findByReceiverEmailAndReplyIsNullOrderBySentAtDesc(tutorEmail)
                .stream().filter(m -> "UNREAD".equals(m.getStatus())).count();
    }
}
