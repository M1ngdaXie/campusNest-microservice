package com.campusnest.messagingservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TypingIndicatorResponse {
    private Long conversationId;
    private Long userId;
    private Boolean isTyping;

    public static TypingIndicatorResponse create(Long conversationId, Long userId, Boolean isTyping) {
        return new TypingIndicatorResponse(conversationId, userId, isTyping);
    }
}