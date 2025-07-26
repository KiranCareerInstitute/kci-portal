package com.kci.portal.repository;

import com.kci.portal.model.ChatMessage;
import com.kci.portal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySenderEmailOrderBySentAtDesc(String senderEmail);

    List<ChatMessage> findByReceiverEmailAndReplyIsNullOrderBySentAtDesc(String receiverEmail);

    List<ChatMessage> findByReceiverEmailAndReplyIsNotNullOrderBySentAtDesc(String receiverEmail);

    List<ChatMessage> findBySenderEmailOrReceiverEmailOrderBySentAtAsc(String senderEmail, String receiverEmail);

    List<ChatMessage> findBySenderEmail(String senderEmail);

    List<ChatMessage> findTop1BySenderEmailOrderBySentAtDesc(String email);

    @Query("SELECT DISTINCT c.senderEmail FROM ChatMessage c WHERE c.senderEmail IS NOT NULL AND c.senderEmail <> 'admin@kci.com'")
    List<String> findDistinctStudentEmails();

    @Query("SELECT c FROM ChatMessage c WHERE c.senderEmail = :studentEmail OR c.receiverEmail = :studentEmail ORDER BY c.sentAt ASC")
    List<ChatMessage> findAllMessagesWithStudent(@Param("studentEmail") String studentEmail);

    boolean existsBySenderEmail(String email);

    boolean existsByReceiverEmail(String email);

    @Query("SELECT DISTINCT u FROM User u WHERE u.email IN (" +
            "SELECT DISTINCT c.senderEmail FROM ChatMessage c WHERE c.senderEmail <> 'admin@kci.com')")
    List<User> findActiveChatSenders();

    @Query("SELECT DISTINCT u FROM User u WHERE u.email IN (" +
            "SELECT DISTINCT c.receiverEmail FROM ChatMessage c WHERE c.reply IS NOT NULL)")
    List<User> findTutorsInvolvedInChat();

    @Query("SELECT DISTINCT u FROM User u WHERE 'STUDENT' MEMBER OF u.roles AND u.email IN " +
            "(SELECT DISTINCT c.senderEmail FROM ChatMessage c)")
    List<User> findStudentsInvolvedInChat();

    @Query("SELECT DISTINCT c.senderEmail FROM ChatMessage c " +
            "UNION SELECT DISTINCT c.receiverEmail FROM ChatMessage c")
    List<String> findAllInvolvedEmails();

    @Query("SELECT DISTINCT c.senderEmail FROM ChatMessage c WHERE c.senderEmail IS NOT NULL AND c.senderEmail <> 'admin@kci.com'")
    List<String> findAllSenderEmails();

    @Query("SELECT m FROM ChatMessage m WHERE m.senderEmail IN (SELECT u.email FROM User u JOIN u.roles r WHERE r = :role)")
    List<ChatMessage> findAllBySenderRole(@Param("role") String role);

    // ✅ Valid session-based message fetch methods
    List<ChatMessage> findBySenderEmailAndReceiverEmailOrderBySentAtAsc(String sender, String receiver);
    List<ChatMessage> findByReceiverEmailAndSenderEmailOrderBySentAtAsc(String receiver, String sender);
}
