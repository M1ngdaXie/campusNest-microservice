package com.campusnest.messagingservice.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    /**
     * Add random jitter to TTL to prevent cache avalanche
     *
     * Cache Avalanche Prevention:
     * When many cache entries expire simultaneously, they cause a stampede to the database.
     * By adding random jitter (±20%), we spread out expiration times.
     *
     * Example: 30 minutes with 20% jitter = 24-36 minutes random range
     *
     * @param baseTtl Base TTL duration
     * @param jitterPercent Percentage of jitter (e.g., 20 for ±20%)
     * @return TTL with random jitter applied
     */
    private Duration addJitter(Duration baseTtl, int jitterPercent) {
        long baseSeconds = baseTtl.getSeconds();
        long jitterSeconds = baseSeconds * jitterPercent / 100;

        // Random value between -jitterSeconds and +jitterSeconds
        long randomJitter = ThreadLocalRandom.current().nextLong(-jitterSeconds, jitterSeconds + 1);

        return Duration.ofSeconds(baseSeconds + randomJitter);
    }

    private ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register Hibernate6Module to handle JPA/Hibernate entities
        Hibernate6Module hibernateModule = new Hibernate6Module();
        hibernateModule.disable(Hibernate6Module.Feature.USE_TRANSIENT_ANNOTATION);
        hibernateModule.disable(Hibernate6Module.Feature.FORCE_LAZY_LOADING);
        hibernateModule.disable(Hibernate6Module.Feature.WRITE_MISSING_ENTITIES_AS_NULL);
        hibernateModule.enable(Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS);
        hibernateModule.enable(Hibernate6Module.Feature.REPLACE_PERSISTENT_COLLECTIONS);

        mapper.registerModule(hibernateModule);
        mapper.registerModule(new JavaTimeModule());

        // Serialization features
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);

        // Visibility settings
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.PUBLIC_ONLY);
        mapper.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.PUBLIC_ONLY);
        mapper.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY);

        // Default typing for Redis values
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        return mapper;
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        return new LettuceConnectionFactory(redisConfig);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper());

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper());

        // Default configuration with jittered TTL (30 minutes ± 20% = 24-36 minutes)
        // IMPORTANT: We now ENABLE null value caching for Cache Penetration Prevention
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(addJitter(Duration.ofMinutes(30), 20))  // Add 20% jitter
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
                // Removed .disableCachingNullValues() to allow caching empty results

        /**
         * Configure different TTLs for different cache types with jitter
         *
         * Cache Avalanche Prevention Strategy:
         * - conversations: 30min ± 20% = 24-36 min
         * - conversation-messages: 15min ± 20% = 12-18 min
         * - unread-counts: 5min ± 20% = 4-6 min
         * - conversation-unread-counts: 5min ± 20% = 4-6 min
         *
         * This prevents all cached items from expiring simultaneously.
         */
        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
                "conversations", defaultConfig.entryTtl(addJitter(Duration.ofMinutes(30), 20)),
                "conversation-messages", defaultConfig.entryTtl(addJitter(Duration.ofMinutes(15), 20)),
                "unread-counts", defaultConfig.entryTtl(addJitter(Duration.ofMinutes(5), 20)),
                "conversation-unread-counts", defaultConfig.entryTtl(addJitter(Duration.ofMinutes(5), 20))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
