package com.tuttime.admin.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Configuration for Admin Service
 * Configures Kafka producer for publishing campaign and template events
 */
@Configuration
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class KafkaConfig {

	private final KafkaProperties kafkaProperties;

	@Value("${spring.kafka.bootstrap-servers}")
	private String bootstrapServers;

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

	@Bean
	public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
		return new KafkaTemplate<>(producerFactory);
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
}
