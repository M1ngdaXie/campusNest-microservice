package com.campusnest.messagingservice.repository;

import com.campusnest.messagingservice.models.Conversation;
import com.campusnest.messagingservice.models.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE m.conversation = :conversation " +
           "ORDER BY m.sentAt ASC")
    List<Message> findByConversationOrderBySentAtAsc(@Param("conversation") Conversation conversation);

    @Query("SELECT m FROM Message m WHERE m.conversation = :conversation " +
           "ORDER BY m.sentAt ASC")
    Page<Message> findByConversationOrderBySentAtAsc(
            @Param("conversation") Conversation conversation,
            Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.conversation = :conversation " +
           "ORDER BY m.sentAt DESC")
    Page<Message> findByConversationOrderBySentAtDesc(
            @Param("conversation") Conversation conversation,
            Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.conversation = :conversation " +
           "AND m.sentAt > :since ORDER BY m.sentAt ASC")
    List<Message> findByConversationAndSentAtAfter(
            @Param("conversation") Conversation conversation,
            @Param("since") LocalDateTime since);

    @Query("SELECT m FROM Message m WHERE m.conversation = :conversation " +
           "ORDER BY m.sentAt DESC LIMIT 1")
    Optional<Message> findLatestMessageByConversation(@Param("conversation") Conversation conversation);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation = :conversation")
    long countByConversation(@Param("conversation") Conversation conversation);

    @Query("SELECT m FROM Message m WHERE m.conversation = :conversation " +
           "AND m.senderId != :userId AND m.id NOT IN " +
           "(SELECT ms.message.id FROM MessageStatus ms WHERE ms.userId = :userId AND ms.status = 'READ')")
    List<Message> findUnreadMessagesInConversation(
            @Param("conversation") Conversation conversation,
            @Param("userId") Long userId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation = :conversation " +
           "AND m.senderId != :userId AND m.id NOT IN " +
           "(SELECT ms.message.id FROM MessageStatus ms WHERE ms.userId = :userId AND ms.status = 'READ')")
    long countUnreadMessagesInConversation(
            @Param("conversation") Conversation conversation,
            @Param("userId") Long userId);

    @Query("SELECT m FROM Message m WHERE m.conversation.id IN " +
           "(SELECT c.id FROM Conversation c WHERE c.participant1Id = :userId OR c.participant2Id = :userId) " +
           "AND m.senderId != :userId AND m.id NOT IN " +
           "(SELECT ms.message.id FROM MessageStatus ms WHERE ms.userId = :userId AND ms.status = 'READ')")
    List<Message> findAllUnreadMessagesForUser(@Param("userId") Long userId);
}