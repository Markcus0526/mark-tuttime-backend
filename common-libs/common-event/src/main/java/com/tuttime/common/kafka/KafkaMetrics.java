package com.tuttime.common.kafka;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Kafka Metrics Collector
 *
 * <p>Provides metrics for Kafka consumer and producer operations:
 * - Consumer lag monitoring
 * - Message processing latency
 * - Error rate tracking
 * - DLQ message counts
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaMetrics {

	private final MeterRegistry meterRegistry;

	// Store lag values per topic/group combination
	private final java.util.Map<String, AtomicLong> lagValues = new java.util.concurrent.ConcurrentHashMap<>();

	// Consumer Metrics
	private static final String CONSUMER_LAG_METRIC = "kafka.consumer.lag";
	private static final String CONSUMER_PROCESSING_TIME_METRIC = "kafka.consumer.processing.time";
	private static final String CONSUMER_MESSAGES_PROCESSED_METRIC = "kafka.consumer.messages.processed";
	private static final String CONSUMER_ERRORS_METRIC = "kafka.consumer.errors";

	// Producer Metrics
	private static final String PRODUCER_MESSAGES_SENT_METRIC = "kafka.producer.messages.sent";
	private static final String PRODUCER_ERRORS_METRIC = "kafka.producer.errors";

	// DLQ Metrics
	private static final String DLQ_MESSAGES_SENT_METRIC = "kafka.dlq.messages.sent";
	private static final String DLQ_RETRY_COUNT_METRIC = "kafka.dlq.retry.count";

	/**
	 * Record consumer lag
	 */
	public void recordConsumerLag(String topic, String groupId, long lag) {
		String key = topic + ":" + groupId;
		AtomicLong lagValue = lagValues.computeIfAbsent(key, k -> {
			// Register gauge on first call - use lambda to read current value
			AtomicLong value = new AtomicLong(0);
			Gauge.builder(CONSUMER_LAG_METRIC, value, AtomicLong::get)
					.description("Consumer lag for Kafka topic")
					.tag("topic", topic)
					.tag("group", groupId)
					.register(meterRegistry);
			return value;
		});
		lagValue.set(lag);
	}

	/**
	 * Record message processing time
	 */
	public Timer.Sample startProcessingTimer() {
		return Timer.start(meterRegistry);
	}

	/**
	 * Record successful message processing
	 */
	public void recordProcessingSuccess(String topic, String groupId, Timer.Sample sample) {
		sample.stop(Timer.builder(CONSUMER_PROCESSING_TIME_METRIC)
				.description("Time taken to process Kafka messages")
				.tag("topic", topic)
				.tag("group", groupId)
				.tag("status", "success")
				.register(meterRegistry));

		Counter.builder(CONSUMER_MESSAGES_PROCESSED_METRIC)
				.description("Number of messages processed successfully")
				.tag("topic", topic)
				.tag("group", groupId)
				.tag("status", "success")
				.register(meterRegistry)
				.increment();
	}

	/**
	 * Record failed message processing
	 */
	public void recordProcessingFailure(String topic, String groupId, Timer.Sample sample, String errorType) {
		// Only stop timer if sample is available (may be null if called from error handler)
		if (sample != null) {
			sample.stop(Timer.builder(CONSUMER_PROCESSING_TIME_METRIC)
					.description("Time taken to process Kafka messages")
					.tag("topic", topic)
					.tag("group", groupId)
					.tag("status", "failure")
					.register(meterRegistry));
		}

		Counter.builder(CONSUMER_ERRORS_METRIC)
				.description("Number of errors during message processing")
				.tag("topic", topic)
				.tag("group", groupId)
				.tag("error_type", errorType)
				.register(meterRegistry)
				.increment();
	}

	/**
	 * Record message sent to DLQ
	 */
	public void recordDlqMessage(String topic, String groupId, int retryCount) {
		Counter.builder(DLQ_MESSAGES_SENT_METRIC)
				.description("Number of messages sent to Dead Letter Queue")
				.tag("topic", topic)
				.tag("group", groupId)
				.register(meterRegistry)
				.increment();

		meterRegistry.counter(DLQ_RETRY_COUNT_METRIC,
						"topic", topic,
						"group", groupId)
				.increment(retryCount);
	}

	/**
	 * Record producer message sent
	 */
	public void recordProducerMessageSent(String topic, boolean success) {
		Counter.builder(PRODUCER_MESSAGES_SENT_METRIC)
				.description("Number of messages sent by producer")
				.tag("topic", topic)
				.tag("status", success ? "success" : "failure")
				.register(meterRegistry)
				.increment();
	}

	/**
	 * Record producer error
	 */
	public void recordProducerError(String topic, String errorType) {
		Counter.builder(PRODUCER_ERRORS_METRIC)
				.description("Number of producer errors")
				.tag("topic", topic)
				.tag("error_type", errorType)
				.register(meterRegistry)
				.increment();
	}
}

