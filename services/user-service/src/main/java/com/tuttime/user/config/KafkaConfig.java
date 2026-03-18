package com.tuttime.user.config;

import com.tuttime.common.kafka.KafkaDlqConfig;
import com.tuttime.common.kafka.KafkaMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Configuration for User Service.
 * 
 * <p>Configures Kafka producer and consumer for publishing and consuming user events.
 * User service publishes user events and consumes user block events from other services.
 */
@Configuration
@ConditionalOnProperty(name = "KAFKA_ENABLED", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;
    private final KafkaMetrics kafkaMetrics;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:user-service-group}")
    private String groupId;

    @Value("${spring.kafka.dlq.topic:${spring.kafka.consumer.group-id}.dlq}")
    private String dlqTopic;

    @Value("${spring.kafka.dlq.max-retries:3}")
    private int maxRetries;

    @Value("${spring.kafka.dlq.initial-retry-delay-ms:1000}")
    private long initialRetryDelayMs;

    @Value("${spring.kafka.dlq.retry-multiplier:2.0}")
    private double retryMultiplier;

    @Value("${spring.kafka.consumer.enable-auto-commit:true}")
    private boolean enableAutoCommit;

    @Value("${spring.kafka.listener.concurrency:3}")
    private int concurrency;

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        
        // Add SASL and security properties from Spring Boot configuration
        addSaslProperties(props);
        
        return new DefaultKafkaProducerFactory<>(props);
    }
    
    /**
     * Adds SASL and security properties from Spring Boot KafkaProperties to the given properties map.
     */
    private void addSaslProperties(Map<String, Object> props) {
        // Get all properties from Spring Boot's KafkaProperties (includes SASL config)
        Map<String, Object> springKafkaProps = kafkaProperties.buildProducerProperties();
        
        // Also check the custom properties map for nested SASL config
        Map<String, String> customProps = kafkaProperties.getProperties();
        
        // Extract SASL-related properties from buildProducerProperties result
        if (springKafkaProps.containsKey(SaslConfigs.SASL_JAAS_CONFIG)) {
            props.put(SaslConfigs.SASL_JAAS_CONFIG, springKafkaProps.get(SaslConfigs.SASL_JAAS_CONFIG));
        } else if (customProps.containsKey("sasl.jaas.config")) {
            props.put(SaslConfigs.SASL_JAAS_CONFIG, customProps.get("sasl.jaas.config"));
        }
        
        if (springKafkaProps.containsKey(SaslConfigs.SASL_MECHANISM)) {
            props.put(SaslConfigs.SASL_MECHANISM, springKafkaProps.get(SaslConfigs.SASL_MECHANISM));
        } else if (customProps.containsKey("sasl.mechanism")) {
            props.put(SaslConfigs.SASL_MECHANISM, customProps.get("sasl.mechanism"));
        }
        
        // Security protocol is at the top level
        if (kafkaProperties.getSecurity() != null && kafkaProperties.getSecurity().getProtocol() != null) {
            props.put("security.protocol", kafkaProperties.getSecurity().getProtocol());
        }
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        // Build SASL properties map from Spring Boot configuration
        Map<String, Object> saslProps = new HashMap<>();
        Map<String, Object> springKafkaProps = kafkaProperties.buildConsumerProperties();
        
        // Also check the custom properties map for nested SASL config
        Map<String, String> customProps = kafkaProperties.getProperties();
        
        // Extract SASL-related properties from buildConsumerProperties result
        if (springKafkaProps.containsKey(SaslConfigs.SASL_JAAS_CONFIG)) {
            saslProps.put(SaslConfigs.SASL_JAAS_CONFIG, springKafkaProps.get(SaslConfigs.SASL_JAAS_CONFIG));
        } else if (customProps.containsKey("sasl.jaas.config")) {
            saslProps.put(SaslConfigs.SASL_JAAS_CONFIG, customProps.get("sasl.jaas.config"));
        }
        
        if (springKafkaProps.containsKey(SaslConfigs.SASL_MECHANISM)) {
            saslProps.put(SaslConfigs.SASL_MECHANISM, springKafkaProps.get(SaslConfigs.SASL_MECHANISM));
        } else if (customProps.containsKey("sasl.mechanism")) {
            saslProps.put(SaslConfigs.SASL_MECHANISM, customProps.get("sasl.mechanism"));
        }
        
        // Security protocol is at the top level
        if (kafkaProperties.getSecurity() != null && kafkaProperties.getSecurity().getProtocol() != null) {
            saslProps.put("security.protocol", kafkaProperties.getSecurity().getProtocol());
        }
        
        // Use KafkaDlqConfig utility if no SASL properties, otherwise create manually
        if (saslProps.isEmpty()) {
            return KafkaDlqConfig.createConsumerFactory(
                    bootstrapServers,
                    groupId,
                    enableAutoCommit
            );
        } else {
            // Create consumer factory manually with SASL properties
            Map<String, Object> props = new HashMap<>();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, enableAutoCommit);
            
            // Error handling deserializers
            props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
            props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, StringDeserializer.class);
            
            // Retry configuration
            props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
            props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
            
            // Add SASL properties
            props.putAll(saslProps);
            
            return new DefaultKafkaConsumerFactory<>(props);
        }
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        
        KafkaDlqConfig.configureDlqErrorHandler(
                factory,
                consumerFactory,
                kafkaTemplate,
                objectMapper,
                dlqTopic,
                enableAutoCommit,
                maxRetries,
                initialRetryDelayMs,
                retryMultiplier,
                concurrency,
                kafkaMetrics
        );
        
        return factory;
    }
}

