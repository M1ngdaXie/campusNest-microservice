package com.campusnest.messagingservice.requests;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateConversationRequest {

    @NotNull(message = "Other participant ID is required")
    private Long otherParticipantId;

    @NotNull(message = "Housing listing ID is required")
    private Long housingListingId;

    private String initialMessage;
}