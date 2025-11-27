package com.campusnest.messagingservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDTO {
    private Long id;
    private Long participant1Id;
    private Long participant2Id;
    private Long housingListingId;
    private UserDTO participant1; // Populated via Feign call
    private UserDTO participant2; // Populated via Feign call
    private HousingListingDTO housingListing; // Populated via Feign call
    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;
    private Boolean isActive;
    private Long unreadCount;
    private MessageDTO lastMessage;
}