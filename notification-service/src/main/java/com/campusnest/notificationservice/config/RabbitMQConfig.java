package com.campusnest.notificationservice.config;

import com.campusnest.common.events.ListingInquiryEvent;
import com.campusnest.common.events.MessageReceivedEvent;
import com.campusnest.common.events.UserRegisteredEvent;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    // Exchange names
    public static final String NOTIFICATIONS_EXCHANGE = "campusnest.notifications";
    public static final String TOPIC_EXCHANGE = "campusnest.notifications.topic";
    public static final String BROADCAST_EXCHANGE = "campusnest.notifications.broadcast";

    // Queue names
    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String NOTIFICATION_DLQ = "notification.queue.dlq";
    public static final String EMAIL_QUEUE = "notification.email.queue";
    public static final String IN_APP_QUEUE = "notification.inapp.queue";

    // Routing keys
    public static final String USER_REGISTERED_KEY = "user.registered";
    public static final String EMAIL_VERIFIED_KEY = "user.email.verified";
    public static final String PASSWORD_CHANGED_KEY = "user.password.changed";
    public static final String MESSAGE_RECEIVED_KEY = "message.received";
    public static final String CONVERSATION_STARTED_KEY = "conversation.started";
    public static final String LISTING_CREATED_KEY = "listing.created";
    public static final String LISTING_UPDATED_KEY = "listing.updated";
    public static final String LISTING_INQUIRY_KEY = "listing.inquiry";

    // Direct Exchange for user-specific notifications
    @Bean
    public DirectExchange notificationsExchange() {
        return new DirectExchange(NOTIFICATIONS_EXCHANGE, true, false);
    }

    // Topic Exchange for pattern-based routing (e.g., listing.*)
    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(TOPIC_EXCHANGE, true, false);
    }

    // Fanout Exchange for broadcast messages
    @Bean
    public FanoutExchange broadcastExchange() {
        return new FanoutExchange(BROADCAST_EXCHANGE, true, false);
    }

    /**
     * Dead Letter Queue (DLQ) for failed notification processing.
     * Messages that fail processing after retries will be sent here for manual inspection.
     */
    @Bean
    public Queue notificationDLQ() {
        return QueueBuilder.durable(NOTIFICATION_DLQ)
                .build();
    }

    /**
     * Main notification queue with DLQ configuration.
     * Failed messages are automatically routed to DLQ after retry attempts.
     */
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument("x-message-ttl", 86400000) // 24 hours TTL
                .withArgument("x-max-length", 10000) // Max queue length
                .withArgument("x-dead-letter-exchange", "") // Use default exchange
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_DLQ) // Route to DLQ
                .build();
    }

    // Email-specific queue
    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
                .withArgument("x-message-ttl", 86400000)
                .build();
    }

    // In-app notification queue
    @Bean
    public Queue inAppQueue() {
        return QueueBuilder.durable(IN_APP_QUEUE)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL for in-app
                .build();
    }

    // Bindings
    @Bean
    public Binding notificationBinding(Queue notificationQueue, DirectExchange notificationsExchange) {
        return BindingBuilder.bind(notificationQueue).to(notificationsExchange).with("notification");
    }

    @Bean
    public Binding userRegisteredBinding(Queue notificationQueue, DirectExchange notificationsExchange) {
        return BindingBuilder.bind(notificationQueue).to(notificationsExchange).with(USER_REGISTERED_KEY);
    }

    @Bean
    public Binding emailVerifiedBinding(Queue notificationQueue, DirectExchange notificationsExchange) {
        return BindingBuilder.bind(notificationQueue).to(notificationsExchange).with(EMAIL_VERIFIED_KEY);
    }

    @Bean
    public Binding passwordChangedBinding(Queue notificationQueue, DirectExchange notificationsExchange) {
        return BindingBuilder.bind(notificationQueue).to(notificationsExchange).with(PASSWORD_CHANGED_KEY);
    }

    @Bean
    public Binding messageReceivedBinding(Queue notificationQueue, DirectExchange notificationsExchange) {
        return BindingBuilder.bind(notificationQueue).to(notificationsExchange).with(MESSAGE_RECEIVED_KEY);
    }

    @Bean
    public Binding conversationStartedBinding(Queue notificationQueue, DirectExchange notificationsExchange) {
        return BindingBuilder.bind(notificationQueue).to(notificationsExchange).with(CONVERSATION_STARTED_KEY);
    }

    @Bean
    public Binding listingCreatedBinding(Queue notificationQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(notificationQueue).to(topicExchange).with("listing.created");
    }

    @Bean
    public Binding listingUpdatedBinding(Queue notificationQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(notificationQueue).to(topicExchange).with("listing.updated");
    }

    @Bean
    public Binding listingInquiryBinding(Queue notificationQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(notificationQueue).to(topicExchange).with("listing.inquiry");
    }

    @Bean
    public Binding listingWildcardBinding(Queue notificationQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(notificationQueue).to(topicExchange).with("listing.#");
    }

    //ClassMapper
    @Bean
    public DefaultClassMapper classMapper(){
        DefaultClassMapper classMapper = new DefaultClassMapper();
        Map<String, Class<?>> idClassMapping = new HashMap<>();

        idClassMapping.put("UserRegisteredEvent", UserRegisteredEvent.class);
        idClassMapping.put("MessageReceivedEvent", MessageReceivedEvent.class);
        idClassMapping.put("ListingInquiryEvent", ListingInquiryEvent.class);

        classMapper.setIdClassMapping(idClassMapping);

        classMapper.setTrustedPackages("com.campusnest.common.events");

        return classMapper;
    }

    // Message converter for JSON
    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setClassMapper(classMapper());
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
