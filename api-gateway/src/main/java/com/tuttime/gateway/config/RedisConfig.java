package com.tuttime.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory; // Standard Blocking Interface
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import reactor.core.publisher.Mono;

import java.util.List;

@Configuration("apiGatewayRedisConfig")
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.sentinel.master:}")
    private String sentinelMaster;

    @Value("${spring.data.redis.sentinel.nodes:}")
    private List<String> sentinelNodes;

    /**
     * 🛑 CORE LETTUCE CONNECTION FACTORY
     * Returns the concrete LettuceConnectionFactory class instance directly.
     * This allows us to use it for both reactive and blocking templates safely.
     */
    @Bean("lettuceConnectionFactory")
    @Primary
    public LettuceConnectionFactory lettuceConnectionFactory() {
        if (sentinelMaster != null && !sentinelMaster.isBlank() && sentinelNodes != null && !sentinelNodes.isEmpty()) {
            RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration();
            sentinelConfig.master(sentinelMaster);

            sentinelNodes.forEach(node -> {
                String[] parts = node.split(":");
                sentinelConfig.sentinel(parts[0], Integer.parseInt(parts[1]));
            });

            if (redisPassword != null && !redisPassword.isBlank()) {
                sentinelConfig.setPassword(redisPassword);
            }
            return new LettuceConnectionFactory(sentinelConfig);
        }

        RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration(redisHost, redisPort);
        if (redisPassword != null && !redisPassword.isBlank()) {
            standaloneConfig.setPassword(redisPassword);
        }
        return new LettuceConnectionFactory(standaloneConfig);
    }

    /**
     * Exposes the Reactive Connection Factory by referencing our primary Lettuce bean.
     */
    @Bean("customReactiveRedisConnectionFactory")
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory(
            @Qualifier("lettuceConnectionFactory") LettuceConnectionFactory factory) {
        return factory;
    }

    @Bean
    public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(
            @Qualifier("customReactiveRedisConnectionFactory") ReactiveRedisConnectionFactory connectionFactory) {

        StringRedisSerializer serializer = new StringRedisSerializer();
        RedisSerializationContext.RedisSerializationContextBuilder<String, String> builder =
                RedisSerializationContext.newSerializationContext(serializer);

        RedisSerializationContext<String, String> context = builder
                .key(serializer)
                .value(serializer)
                .hashKey(serializer)
                .hashValue(serializer)
                .build();

        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }

    /**
     * 🛑 FIXED COMPILATION BLOCK:
     * Injects the core LettuceConnectionFactory as a standard RedisConnectionFactory type.
     * This fixes the autowire type-mismatch error and resolves the dependency crash inside UserEventConsumer!
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(
            @Qualifier("lettuceConnectionFactory") RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return mapper;
    }

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isEmpty()) {
                return Mono.just(userId);
            }
            String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            return Mono.just(ip);
        };
    }
}
