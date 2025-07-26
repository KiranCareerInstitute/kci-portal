package com.kci.portal.controller;

import com.kci.portal.model.ChatMessage;
import com.kci.portal.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/chat")
public class CommonChatController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @GetMapping("/session/{studentEmail}/{tutorEmail}")
    public String openSharedSession(@PathVariable String studentEmail,
                                    @PathVariable String tutorEmail,
                                    Model model) {
        List<ChatMessage> sent = chatMessageRepository
                .findBySenderEmailAndReceiverEmailOrderBySentAtAsc(studentEmail, tutorEmail);

        List<ChatMessage> received = chatMessageRepository
                .findByReceiverEmailAndSenderEmailOrderBySentAtAsc(studentEmail, tutorEmail);

        List<ChatMessage> fullChat = new ArrayList<>();
        fullChat.addAll(sent);
        fullChat.addAll(received);
        fullChat.sort(Comparator.comparing(ChatMessage::getSentAt));

        model.addAttribute("messages", fullChat);
        model.addAttribute("studentEmail", studentEmail);
        model.addAttribute("tutorEmail", tutorEmail);

        return "student/chat-room"; // ✅ Shared view for both tutor and student
    }
}
