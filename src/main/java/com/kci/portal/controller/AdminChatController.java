package com.kci.portal.controller;

import com.kci.portal.model.User;
import com.kci.portal.model.ChatMessage;
import com.kci.portal.repository.ChatMessageRepository;
import com.kci.portal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/chat")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminChatController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    // ✅ Show chat threads (students who have chatted)
    @GetMapping("/threads")
    public String showAllChatThreads(Model model) {
        List<String> chatEmails = chatMessageRepository.findAllSenderEmails();

        List<User> students = userRepository.findAll().stream()
                .filter(user -> user.getRoles().contains("ROLE_USER")) // ✅ student role
                .filter(user -> chatEmails.contains(user.getEmail()))
                .toList();

        model.addAttribute("students", students);
        return "admin/admin-view-chat-threads";
    }

    // ✅ View chat history with a student
    @GetMapping("/threads/{studentEmail}")
    public String viewStudentChat(@PathVariable String studentEmail, Model model) {
        List<ChatMessage> messages = chatMessageRepository.findAllMessagesWithStudent(studentEmail);
        model.addAttribute("messages", messages);
        model.addAttribute("studentEmail", studentEmail);

        User student = userRepository.findByEmail(studentEmail).orElse(null);
        model.addAttribute("studentUser", student);

        boolean isRestricted = student != null && student.isRestrictedFromChat();
        model.addAttribute("isRestricted", isRestricted);

        return "admin/admin-view-chat-detail";
    }

    // ✅ Admin reply to student
    @PostMapping("/reply/{studentEmail}")
    public String sendReply(@PathVariable String studentEmail,
                            @RequestParam("replyMessage") String replyMessage) {
        ChatMessage reply = new ChatMessage();
        reply.setSenderEmail("admin@kci.com");
        reply.setReceiverEmail(studentEmail);
        reply.setMessage(replyMessage);
        reply.setSentAt(LocalDateTime.now());
        reply.setStatus("NOTICE");
        chatMessageRepository.save(reply);

        userRepository.findByEmail(studentEmail).ifPresent(student -> {
            student.setFlashNotice("🔔 Admin replied: \"" + replyMessage + "\"");
            userRepository.save(student);
        });

        return "redirect:/admin/chat/threads/" + studentEmail;
    }

    // ✅ Restrict student from chat
    @PostMapping("/restrict/{studentEmail}")
    public String restrictStudent(@PathVariable String studentEmail) {
        List<ChatMessage> messages = chatMessageRepository.findBySenderEmail(studentEmail);
        for (ChatMessage msg : messages) {
            msg.setRestricted(true);
            chatMessageRepository.save(msg);
        }

        userRepository.findByEmail(studentEmail).ifPresent(user -> {
            user.setRestrictedFromChat(true);
            userRepository.save(user);
        });

        return "redirect:/admin/chat/threads/" + studentEmail;
    }

    // ✅ Unrestrict student from chat
    @PostMapping("/unrestrict/{studentEmail}")
    public String unrestrictStudent(@PathVariable String studentEmail) {
        List<ChatMessage> messages = chatMessageRepository.findBySenderEmail(studentEmail);
        for (ChatMessage msg : messages) {
            msg.setRestricted(false);
            chatMessageRepository.save(msg);
        }

        userRepository.findByEmail(studentEmail).ifPresent(user -> {
            user.setRestrictedFromChat(false);
            userRepository.save(user);
        });

        return "redirect:/admin/chat/threads/" + studentEmail;
    }

    // ✅ View all active chat users (students + tutors)
    @GetMapping("/active")
    public String showActiveChatUsers(Model model) {
        List<User> allSenders = chatMessageRepository.findActiveChatSenders();

        List<User> students = allSenders.stream()
                .filter(u -> u.hasRole("STUDENT"))
                .toList();

        List<User> tutors = allSenders.stream()
                .filter(u -> u.hasRole("TUTOR"))
                .toList();

        model.addAttribute("students", students);
        model.addAttribute("tutors", tutors);
        return "admin/admin-active-chat-users";
    }
    // ✅ Show chat threads (tutors who have chatted)
    @GetMapping("/tutor-threads")
    public String showTutorChatThreads(Model model) {
        List<User> tutors = userRepository.findAll().stream()
                .filter(u -> u.hasRole("ROLE_TUTOR"))
                .filter(u -> chatMessageRepository.existsBySenderEmail(u.getEmail()) ||
                        chatMessageRepository.existsByReceiverEmail(u.getEmail()))
                .toList();

        model.addAttribute("tutors", tutors);
        return "admin/admin-view-tutor-threads";
    }

}
