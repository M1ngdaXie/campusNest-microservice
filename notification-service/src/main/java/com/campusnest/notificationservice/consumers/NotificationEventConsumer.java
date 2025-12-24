package com.campusnest.notificationservice.consumers;

  import com.campusnest.notificationservice.config.RabbitMQConfig;
  import com.campusnest.common.events.UserRegisteredEvent;
  import com.campusnest.common.events.MessageReceivedEvent;
  import com.campusnest.common.events.ListingInquiryEvent;
  import com.campusnest.notificationservice.services.NotificationService;
  import lombok.extern.slf4j.Slf4j;
  import org.springframework.amqp.rabbit.annotation.RabbitHandler;
  import org.springframework.amqp.rabbit.annotation.RabbitListener;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.stereotype.Component;

  @Component
  @Slf4j
  @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
  public class NotificationEventConsumer {

      @Autowired
      private NotificationService notificationService;
      @RabbitHandler
      public void handleUserRegistered(UserRegisteredEvent event) {
          try {
              log.info("🎉 User registered event received: userId={}, email={}",
                      event.getUserId(), event.getEmail());
              notificationService.handleUserRegistered(event);
          } catch (Exception e) {
              log.error("❌ Error processing UserRegisteredEvent: {}", e.getMessage(), e);
              throw e; // Re-throw to trigger retry mechanism
          }
      }

      @RabbitHandler
      public void handleMessageReceived(MessageReceivedEvent event) {
          try {
              log.info("💬 Message received event: recipientId={}, senderId={}, conversationId={}",
                      event.getRecipientUserId(), event.getSenderUserId(), event.getConversationId());
              notificationService.handleMessageReceived(event);
              log.info("✅ Message received event processed successfully");
          } catch (Exception e) {
              log.error("❌ Error processing MessageReceivedEvent: {}", e.getMessage(), e);
              throw e;
          }
      }

      @RabbitHandler
      public void handleListingInquiry(ListingInquiryEvent event) {
          try {
              log.info("🏠 Listing inquiry event: landlordId={}, listingId={}, inquirerId={}",
                      event.getLandlordUserId(), event.getListingId(), event.getInquirerUserId());
              notificationService.handleListingInquiry(event);
              log.info("✅ Listing inquiry event processed successfully");
          } catch (Exception e) {
              log.error("❌ Error processing ListingInquiryEvent: {}", e.getMessage(), e);
              throw e;
          }
      }
      @RabbitHandler(isDefault = true)
      public void handleUnknown(Object event){
          log.warn("⚠️ Received unknown event type: {}, event: {}",
                  event.getClass().getName(), event);
      }
  }