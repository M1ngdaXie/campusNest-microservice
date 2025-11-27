package com.campusnest.messagingservice.dto;

import com.campusnest.messagingservice.enums.MessageType;
import com.campusnest.messagingservice.models.Message;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String content;
    private MessageType messageType;
    private LocalDateTime sentAt;
    private Boolean isEdited;

    public static ChatMessageResponse fromMessage(Message message) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setId(message.getId());
        response.setConversationId(message.getConversation().getId());
        response.setSenderId(message.getSenderId());
        response.setContent(message.getContent());
        response.setMessageType(message.getMessageType());
        response.setSentAt(message.getSentAt());
        response.setIsEdited(message.getIsEdited());
        return response;
    }
}