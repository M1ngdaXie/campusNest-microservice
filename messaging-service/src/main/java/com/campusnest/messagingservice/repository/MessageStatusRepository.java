package com.campusnest.messagingservice.repository;

import com.campusnest.messagingservice.enums.MessageStatusType;
import com.campusnest.messagingservice.models.Message;
import com.campusnest.messagingservice.models.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageStatusRepository extends JpaRepository<MessageStatus, Long> {

    @Query("SELECT ms FROM MessageStatus ms WHERE ms.message = :message AND ms.userId = :userId")
    List<MessageStatus> findByMessageAndUserId(
            @Param("message") Message message,
            @Param("userId") Long userId);

    @Query("SELECT ms FROM MessageStatus ms WHERE ms.message = :message AND ms.userId = :userId AND ms.status = :status")
    Optional<MessageStatus> findByMessageAndUserIdAndStatus(
            @Param("message") Message message,
            @Param("userId") Long userId,
            @Param("status") MessageStatusType status);

    @Query("SELECT ms FROM MessageStatus ms WHERE ms.message = :message")
    List<MessageStatus> findByMessage(@Param("message") Message message);

    @Query("SELECT ms FROM MessageStatus ms WHERE ms.message IN :messages AND ms.userId = :userId AND ms.status = :status")
    List<MessageStatus> findByMessagesAndUserIdAndStatus(
            @Param("messages") List<Message> messages,
            @Param("userId") Long userId,
            @Param("status") MessageStatusType status);

    @Query("SELECT CASE WHEN COUNT(ms) > 0 THEN true ELSE false END " +
           "FROM MessageStatus ms WHERE ms.message = :message AND ms.userId = :userId AND ms.status = :status")
    boolean existsByMessageAndUserIdAndStatus(
            @Param("message") Message message,
            @Param("userId") Long userId,
            @Param("status") MessageStatusType status);

    @Query("SELECT ms FROM MessageStatus ms WHERE ms.userId = :userId AND ms.status = :status")
    List<MessageStatus> findByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") MessageStatusType status);

    void deleteByMessageAndUserId(Message message, Long userId);
}