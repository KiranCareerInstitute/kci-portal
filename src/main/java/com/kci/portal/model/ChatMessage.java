package com.kci.portal.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_message")
public class ChatMessage {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   private String senderEmail;
   private String receiverEmail;

   @Column(length = 2000)
   private String message;

   @Column(length = 20)
   private String status = "UNREAD"; // NEW field

   @Column(name = "restricted", nullable = false)
   private boolean restricted = false; // ✅ NEW FIELD (used for admin control)

   private LocalDateTime sentAt;

   private String reply; // Optional reply from tutor

   private LocalDateTime repliedAt;

   @Column(name = "file_name")
   private String fileName;

   @Column(name = "file_path")
   private String filePath;


   // === Getters and Setters ===
   public String getFileName() {
      return fileName;
   }

   public void setFileName(String fileName) {
      this.fileName = fileName;
   }

   public String getFilePath() {
      return filePath;
   }

   public void setFilePath(String filePath) {
      this.filePath = filePath;
   }
   public Long getId() { return id; }

   public void setId(Long id) { this.id = id; }

   public String getSenderEmail() { return senderEmail; }

   public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }

   public String getReceiverEmail() { return receiverEmail; }

   public void setReceiverEmail(String receiverEmail) { this.receiverEmail = receiverEmail; }

   public String getMessage() { return message; }

   public void setMessage(String message) { this.message = message; }

   public String getStatus() { return status; }

   public void setStatus(String status) { this.status = status; }

   public boolean isRestricted() { return restricted; }

   public void setRestricted(boolean restricted) { this.restricted = restricted; }

   public LocalDateTime getSentAt() { return sentAt; }

   public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

   public String getReply() { return reply; }

   public void setReply(String reply) { this.reply = reply; }

   public LocalDateTime getRepliedAt() { return repliedAt; }

   public void setRepliedAt(LocalDateTime repliedAt) { this.repliedAt = repliedAt; }
}
