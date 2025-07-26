package com.kci.portal.controller;

import com.kci.portal.model.ChatMessage;
import com.kci.portal.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatNotificationController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @GetMapping("/unread-count")
    public int getUnreadMessageCount(Principal principal) {
        String userEmail = principal.getName();

        List<ChatMessage> messages = chatMessageRepository.findBySenderEmailOrderBySentAtDesc(userEmail);
        return (int) messages.stream()
                .filter(m -> "UNREAD".equalsIgnoreCase(m.getStatus()))
                .count();
    }

    @GetMapping("/replied-count")
    public int getRepliedMessageCount(Principal principal) {
        String studentEmail = principal.getName();

        List<ChatMessage> messages = chatMessageRepository.findBySenderEmailOrderBySentAtDesc(studentEmail);
        return (int) messages.stream()
                .filter(m -> "REPLIED".equalsIgnoreCase(m.getStatus()))
                .count();
    }
}
