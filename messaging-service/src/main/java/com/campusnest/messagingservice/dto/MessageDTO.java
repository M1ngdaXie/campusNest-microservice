package com.campusnest.messagingservice.dto;

import com.campusnest.messagingservice.enums.MessageType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private UserDTO sender; // Populated via Feign call
    private String content;
    private MessageType messageType;
    private LocalDateTime sentAt;
    private Boolean isEdited;
    private LocalDateTime editedAt;
}