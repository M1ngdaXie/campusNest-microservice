package com.campusnest.messagingservice.repository;

import com.campusnest.messagingservice.models.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT c FROM Conversation c WHERE " +
           "((c.participant1Id = :userId1 AND c.participant2Id = :userId2) OR " +
           "(c.participant1Id = :userId2 AND c.participant2Id = :userId1)) AND " +
           "c.housingListingId = :listingId AND c.isActive = true")
    Optional<Conversation> findByParticipantsAndListing(
            @Param("userId1") Long userId1,
            @Param("userId2") Long userId2,
            @Param("listingId") Long listingId);

    @Query("SELECT c FROM Conversation c WHERE " +
           "(c.participant1Id = :userId OR c.participant2Id = :userId) AND " +
           "c.isActive = true ORDER BY c.lastMessageAt DESC")
    Page<Conversation> findByUserIdOrderByLastMessageDesc(
            @Param("userId") Long userId,
            Pageable pageable);

    @Query("SELECT c FROM Conversation c WHERE " +
           "(c.participant1Id = :userId OR c.participant2Id = :userId) AND " +
           "c.isActive = true ORDER BY c.lastMessageAt DESC")
    List<Conversation> findByUserIdOrderByLastMessageDesc(@Param("userId") Long userId);

    @Query("SELECT c FROM Conversation c WHERE " +
           "c.housingListingId = :listingId AND c.isActive = true")
    List<Conversation> findByHousingListingIdAndActiveTrue(@Param("listingId") Long listingId);

    @Query("SELECT COUNT(c) FROM Conversation c WHERE " +
           "(c.participant1Id = :userId OR c.participant2Id = :userId) AND " +
           "c.isActive = true")
    long countActiveConversationsByUserId(@Param("userId") Long userId);

    @Query("SELECT c FROM Conversation c WHERE " +
           "c.id = :conversationId AND " +
           "(c.participant1Id = :userId OR c.participant2Id = :userId)")
    Optional<Conversation> findByIdAndParticipantId(
            @Param("conversationId") Long conversationId,
            @Param("userId") Long userId);
}