package com.tuttime.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.event.DlqErrorHandler;
import com.tuttime.common.event.KafkaMetrics;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Dead Letter Queue Configuration Utility
 *
 * <p>Provides utility methods for creating Kafka configurations with DLQ and retry support.
 * Services should create their own KafkaConfig that uses these utilities.
 */
@Slf4j
public class KafkaDlqConfig {

	/**
	 * Creates a consumer factory with error handling deserializers.
	 *
	 * @param bootstrapServers Kafka bootstrap servers
	 * @param groupId Consumer group ID
	 * @param enableAutoCommit Whether to enable auto-commit
	 * @param additionalProperties Additional Kafka properties (e.g., SASL configuration)
	 */
	public static ConsumerFactory<String, String> createConsumerFactory(
			String bootstrapServers,
			String groupId,
			boolean enableAutoCommit,
			Map<String, Object> additionalProperties) {

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
		props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 720000); // 12 min — must exceed FFmpeg processing timeout (600s)

		// Add additional properties (e.g., SASL configuration)
		if (additionalProperties != null && !additionalProperties.isEmpty()) {
			props.putAll(additionalProperties);
			log.debug("Added {} additional properties to Kafka consumer configuration", additionalProperties.size());
		}

		return new DefaultKafkaConsumerFactory<>(props);
	}

	/**
	 * Creates a consumer factory with error handling deserializers.
	 * Backward compatibility method without additional properties.
	 */
	public static ConsumerFactory<String, String> createConsumerFactory(
			String bootstrapServers,
			String groupId,
			boolean enableAutoCommit) {
		return createConsumerFactory(bootstrapServers, groupId, enableAutoCommit, null);
	}

	/**
	 * Creates a DLQ error handler.
	 */
	public static CommonErrorHandler createDlqErrorHandler(
			KafkaTemplate<String, String> kafkaTemplate,
			ObjectMapper objectMapper,
			String dlqTopic,
			int maxRetries,
			long initialRetryDelayMs,
			double retryMultiplier,
			KafkaMetrics kafkaMetrics) {

		return new DlqErrorHandler(
				kafkaTemplate,
				objectMapper,
				dlqTopic,
				maxRetries,
				initialRetryDelayMs,
				retryMultiplier,
				kafkaMetrics
		);
	}

	/**
	 * Configures a listener container factory with DLQ error handler.
	 */
	public static void configureDlqErrorHandler(
			ConcurrentKafkaListenerContainerFactory<String, String> factory,
			ConsumerFactory<String, String> consumerFactory,
			KafkaTemplate<String, String> kafkaTemplate,
			ObjectMapper objectMapper,
			String dlqTopic,
			boolean enableAutoCommit,
			int maxRetries,
			long initialRetryDelayMs,
			double retryMultiplier,
			int concurrency,
			KafkaMetrics kafkaMetrics) {

		factory.setConsumerFactory(consumerFactory);

		// Set concurrency (number of concurrent consumer threads)
		factory.setConcurrency(concurrency);

		// Set error handler with DLQ support
		CommonErrorHandler errorHandler = createDlqErrorHandler(
				kafkaTemplate,
				objectMapper,
				dlqTopic,
				maxRetries,
				initialRetryDelayMs,
				retryMultiplier,
				kafkaMetrics
		);
		factory.setCommonErrorHandler(errorHandler);

		log.info("Configured Kafka listener with DLQ topic: {}, max retries: {}, concurrency: {}",
				dlqTopic, maxRetries, concurrency);
	}
}

