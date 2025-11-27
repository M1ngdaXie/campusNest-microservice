package com.campusnest.messagingservice.services;

import com.campusnest.messagingservice.clients.HousingServiceClient;
import com.campusnest.messagingservice.clients.UserServiceClient;
import com.campusnest.messagingservice.dto.HousingListingDTO;
import com.campusnest.messagingservice.dto.UserDTO;
import com.campusnest.messagingservice.enums.MessageStatusType;
import com.campusnest.messagingservice.enums.MessageType;
import com.campusnest.messagingservice.exceptions.ConversationNotFoundException;
import com.campusnest.messagingservice.exceptions.InvalidMessageException;
import com.campusnest.messagingservice.exceptions.MessageNotFoundException;
import com.campusnest.messagingservice.exceptions.ServiceUnavailableException;
import com.campusnest.messagingservice.exceptions.UnauthorizedAccessException;
import com.campusnest.messagingservice.models.Conversation;
import com.campusnest.messagingservice.models.Message;
import com.campusnest.messagingservice.models.MessageStatus;
import com.campusnest.messagingservice.repository.ConversationRepository;
import com.campusnest.messagingservice.repository.MessageRepository;
import com.campusnest.messagingservice.repository.MessageStatusRepository;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@Transactional
public class MessagingServiceImpl implements MessagingService {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageStatusRepository messageStatusRepository;

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private HousingServiceClient housingServiceClient;

    @Override
    public Conversation createOrGetConversation(Long user1Id, Long user2Id, Long listingId) {
        log.info("Creating or getting conversation between users {} and {} for listing {}",
                user1Id, user2Id, listingId);

        // Prevent users from creating conversations with themselves
        if (user1Id.equals(user2Id)) {
            throw new IllegalArgumentException("Users cannot create conversations with themselves");
        }

        // Validate that both users exist via Feign client
        UserDTO user2;
        try {
            user2 = userServiceClient.getUserById(user2Id);
            if (user2 == null) {
                throw new IllegalArgumentException("User not found with ID: " + user2Id);
            }
        } catch (FeignException e) {
            int status = e.status();
            log.error("Feign exception when fetching user {}: status={}, message={}", user2Id, status, e.getMessage());

            // 4xx errors are client errors (user not found, forbidden, etc.)
            if (status >= 400 && status < 500) {
                log.error("User {} not found or not accessible in user-service (HTTP {})", user2Id, status);
                throw new IllegalArgumentException("User not found with ID: " + user2Id);
            }
            // 5xx errors or unknown errors are server errors (service unavailable)
            log.error("Failed to fetch user {} from user-service (HTTP {}): {}", user2Id, status, e.getMessage());
            throw new ServiceUnavailableException("user-service", e);
        }

        log.info("Validated user: {} {}", user2.getFirstName(), user2.getLastName());

        // Validate housing listing exists and is active via Feign client
        HousingListingDTO listing;
        try {
            listing = housingServiceClient.getHousingListingById(listingId);
            if (listing == null) {
                throw new IllegalArgumentException("Housing listing not found with ID: " + listingId);
            }
        } catch (FeignException.NotFound e) {
            log.error("Housing listing {} not found in housing-service", listingId);
            throw new IllegalArgumentException("Housing listing not found with ID: " + listingId);
        } catch (FeignException e) {
            log.error("Failed to fetch housing listing {} from housing-service: {}", listingId, e.getMessage());
            throw new ServiceUnavailableException("housing-service", e);
        }

        log.info("Validated housing listing: {}", listing.getTitle());

        return conversationRepository.findByParticipantsAndListing(user1Id, user2Id, listingId)
                .orElseGet(() -> {
                    log.info("Creating new conversation");
                    Conversation conversation = new Conversation();
                    conversation.setParticipant1Id(user1Id);
                    conversation.setParticipant2Id(user2Id);
                    conversation.setHousingListingId(listingId);
                    conversation.setIsActive(true);

                    Conversation saved = conversationRepository.save(conversation);

                    // Send system message to initialize conversation
                    sendMessage(saved.getId(), user1Id,
                            "Conversation started about: " + listing.getTitle(),
                            MessageType.SYSTEM);

                    return saved;
                });
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "unread-counts", allEntries = true),
            @CacheEvict(value = "conversation-unread-counts", allEntries = true),
            @CacheEvict(value = "conversations", allEntries = true),
            @CacheEvict(value = "conversation-messages", key = "#conversationId")
    })
    public Message sendMessage(Long conversationId, Long senderId, String content, MessageType messageType) {
        log.info("Sending {} message in conversation {} from user {}",
                messageType, conversationId, senderId);

        // Validate message content
        if (content == null || content.trim().isEmpty()) {
            throw new InvalidMessageException("Message content cannot be empty");
        }

        if (content.length() > 5000) {
            throw new InvalidMessageException("Message content must be between 1 and 5000 characters");
        }

        Conversation conversation = getConversation(conversationId, senderId);

        Message message = new Message();
        message.setConversation(conversation);
        message.setSenderId(senderId);
        message.setContent(content);
        message.setMessageType(messageType);
        message.setSentAt(LocalDateTime.now());

        Message savedMessage = messageRepository.save(message);

        // Update conversation last message time
        conversation.updateLastMessageTime();
        conversationRepository.save(conversation);

        // Create message status for sender (SENT)
        MessageStatus senderStatus = MessageStatus.createSentStatus(savedMessage, senderId);
        messageStatusRepository.save(senderStatus);

        // Create message status for recipient (DELIVERED)
        Long recipientId = conversation.getOtherParticipantId(senderId);
        if (recipientId != null) {
            MessageStatus recipientStatus = MessageStatus.createDeliveredStatus(savedMessage, recipientId);
            messageStatusRepository.save(recipientStatus);
        }

        log.info("Message sent successfully with ID: {}", savedMessage.getId());
        return savedMessage;
    }

    @Override
    public Message sendMessage(Long conversationId, Long senderId, String content) {
        return sendMessage(conversationId, senderId, content, MessageType.TEXT);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Message> getConversationMessages(Long conversationId, Long requestingUserId, Pageable pageable) {
        log.debug("Getting messages for conversation {} for user {}", conversationId, requestingUserId);

        Conversation conversation = getConversation(conversationId, requestingUserId);
        return messageRepository.findByConversationOrderBySentAtDesc(conversation, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getConversationMessages(Long conversationId, Long requestingUserId) {
        log.debug("Getting all messages for conversation {} for user {}", conversationId, requestingUserId);

        Conversation conversation = getConversation(conversationId, requestingUserId);
        return messageRepository.findByConversationOrderBySentAtAsc(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getRecentMessages(Long conversationId, Long requestingUserId, LocalDateTime since) {
        log.debug("Getting recent messages since {} for conversation {}", since, conversationId);

        Conversation conversation = getConversation(conversationId, requestingUserId);
        return messageRepository.findByConversationAndSentAtAfter(conversation, since);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Conversation> getUserConversations(Long userId) {
        log.debug("Getting conversations for user {}", userId);
        return conversationRepository.findByUserIdOrderByLastMessageDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Conversation> getUserConversations(Long userId, Pageable pageable) {
        log.debug("Getting conversations with pagination for user {}", userId);
        return conversationRepository.findByUserIdOrderByLastMessageDesc(userId, pageable);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "unread-counts", key = "#userId"),
            @CacheEvict(value = "conversation-unread-counts", key = "#conversationId + ':' + #userId")
    })
    @Transactional(noRollbackFor = org.springframework.dao.DataIntegrityViolationException.class)
    public synchronized void markMessagesAsRead(Long conversationId, Long userId) {
        log.info("Marking messages as read in conversation {} for user {}", conversationId, userId);

        Conversation conversation = getConversation(conversationId, userId);
        List<Message> unreadMessages = messageRepository.findUnreadMessagesInConversation(conversation, userId);

        int markedCount = 0;
        for (Message message : unreadMessages) {
            try {
                if (!messageStatusRepository.existsByMessageAndUserIdAndStatus(message, userId, MessageStatusType.READ)) {
                    MessageStatus readStatus = MessageStatus.createReadStatus(message, userId);
                    messageStatusRepository.save(readStatus);
                    markedCount++;
                }
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                log.debug("Message {} already marked as read for user {} (concurrent update)",
                        message.getId(), userId);
            }
        }

        log.info("Marked {} messages as read (out of {} unread)", markedCount, unreadMessages.size());
    }

    @Override
    public void markMessageAsRead(Long messageId, Long userId) {
        log.info("Marking message {} as read for user {}", messageId, userId);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));

        // Verify user can access this message
        if (!message.getConversation().isParticipant(userId)) {
            throw new UnauthorizedAccessException("User " + userId + " not authorized to access message " + messageId);
        }

        if (!messageStatusRepository.existsByMessageAndUserIdAndStatus(message, userId, MessageStatusType.READ)) {
            MessageStatus readStatus = MessageStatus.createReadStatus(message, userId);
            messageStatusRepository.save(readStatus);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "conversation-unread-counts", key = "#conversationId + ':' + #userId")
    public long getUnreadMessageCount(Long conversationId, Long userId) {
        Conversation conversation = getConversation(conversationId, userId);
        return messageRepository.countUnreadMessagesInConversation(conversation, userId);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "unread-counts", key = "#userId")
    public long getTotalUnreadMessageCount(Long userId) {
        List<Message> unreadMessages = messageRepository.findAllUnreadMessagesForUser(userId);
        return unreadMessages.size();
    }

    @Override
    @Transactional(readOnly = true)
    public Conversation getConversation(Long conversationId, Long userId) {
        return conversationRepository.findByIdAndParticipantId(conversationId, userId)
                .orElseThrow(() -> new ConversationNotFoundException(
                        "Conversation not found with ID " + conversationId + " or user " + userId + " not authorized"));
    }


    @Override
    @Transactional(readOnly = true)
    public boolean canUserAccessConversation(Long conversationId, Long userId) {
        return conversationRepository.findByIdAndParticipantId(conversationId, userId).isPresent();
    }

    @Override
    public void deactivateConversation(Long conversationId, Long userId) {
        log.info("Deactivating conversation {} for user {}", conversationId, userId);

        Conversation conversation = getConversation(conversationId, userId);
        conversation.setIsActive(false);
        conversationRepository.save(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public Message getLatestMessage(Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        return messageRepository.findLatestMessageByConversation(conversation)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public void validateConversationIntegrity(Long conversationId, Long userId) {
        Conversation conversation = getConversation(conversationId, userId);

        // Check if housing listing still exists via Feign client
        if (conversation.getHousingListingId() != null) {
            try {
                HousingListingDTO listing = housingServiceClient.getHousingListingById(conversation.getHousingListingId());

                if (listing == null) {
                    log.warn("Conversation {} references deleted housing listing {}",
                            conversationId, conversation.getHousingListingId());
                    throw new IllegalStateException("The housing listing for this conversation no longer exists");
                }
            } catch (Exception e) {
                log.error("Failed to validate housing listing for conversation {}: {}",
                        conversationId, e.getMessage());
                throw new IllegalStateException("Unable to validate housing listing for this conversation");
            }
        }
    }
}
