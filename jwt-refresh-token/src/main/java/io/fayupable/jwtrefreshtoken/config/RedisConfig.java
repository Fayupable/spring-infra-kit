package io.fayupable.jwtrefreshtoken.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

/**
 * Redis Configuration
 * <p>
 * Basic Redis setup for token blacklist.
 * Spring Boot 4.0+ compatible (no deprecated APIs).
 * <p>
 * Conditional Loading:
 * Only loads if Redis is configured in application.yml.
 * <p>
 * Checks:
 * - spring.data.redis.host is set?
 * - If YES → Load this config
 * - If NO → Skip (use InMemoryTokenBlacklistService)
 * <p>
 * RedisTemplate Usage:
 * - TokenBlacklistService uses this for blacklisting
 * - Key: String (token)
 * - Value: Object (usually just "1")
 * <p>
 * Serialization (Spring Boot 4.0+):
 * - Keys: String (plain text)
 * - Values: JSON (via RedisSerializer.json())
 * <p>
 * Spring Boot 4.0+ API:
 * - Uses RedisSerializer.json() factory method
 * - Automatically uses ObjectMapper bean
 * - No deprecated APIs
 * - Clean and simple
 */
@Configuration
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisConfig {

    /**
     * Redis Template Bean
     * <p>
     * Configures RedisTemplate for token blacklist operations.
     * <p>
     * Spring Boot 4.0+ Approach:
     * - RedisSerializer.json() creates Jackson serializer
     * - Automatically uses Jackson 3.x (tools.jackson)
     * - Uses ObjectMapper from Spring context
     * - Type-safe serialization
     * <p>
     * Why RedisTemplate?
     * - Easy to use API
     * - Built-in serialization
     * - Connection pooling (via RedisConnectionFactory)
     * - Auto-reconnection
     * - Thread-safe
     * <p>
     * Serializers:
     * - Key: StringRedisSerializer (plain text)
     * - Value: RedisSerializer.json() (JSON via Jackson 3.x)
     * <p>
     * Storage Format in Redis:
     * Key: "blacklist:eyJhbGci..."  (String)
     * Value: "1"  (JSON string)
     * TTL: 900 seconds (auto-expires)
     *
     * @param connectionFactory Auto-configured by Spring Boot
     * @param objectMapper      Auto-configured by Spring Boot (Jackson 3.x)
     * @return Configured RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // String serializer for keys
        // Used for: "blacklist:TOKEN"
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // JSON serializer for values (Spring Boot 4.0+ way)
        // RedisSerializer.json() automatically:
        // - Uses ObjectMapper from Spring context
        // - Configures Jackson 3.x serialization
        // - Handles type information
        RedisSerializer<Object> jsonSerializer = RedisSerializer.json();

        // Set serializers for all operations
        template.setKeySerializer(stringSerializer);           // Normal keys
        template.setHashKeySerializer(stringSerializer);       // Hash field names
        template.setValueSerializer(jsonSerializer);           // Normal values
        template.setHashValueSerializer(jsonSerializer);       // Hash field values

        // Enable default serialization fallback
        template.setEnableDefaultSerializer(true);

        // Initialize the template
        template.afterPropertiesSet();

        return template;
    }
}