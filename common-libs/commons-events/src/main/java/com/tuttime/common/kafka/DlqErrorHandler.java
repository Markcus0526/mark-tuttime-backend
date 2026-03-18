package com.tuttime.common.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * Dead Letter Queue Error Handler
 * 
 * <p>Handles failed Kafka messages by sending them to a DLQ topic.
 * Uses Spring Kafka's SeekToCurrentErrorHandler for retries with exponential backoff,
 * then sends failed messages to DLQ after max retries.
 */
@Slf4j
public class DlqErrorHandler implements CommonErrorHandler {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String dlqTopic;
    private final int maxRetries;
    private final long initialRetryDelayMs;
    private final double retryMultiplier;
    private final KafkaMetrics kafkaMetrics; // Optional - will be null if not available
    
    // Track retry counts per message (in-memory, will be lost on restart)
    private final Map<String, Integer> retryCounts = new java.util.concurrent.ConcurrentHashMap<>();

    public DlqErrorHandler(KafkaTemplate<String, String> kafkaTemplate, 
                          ObjectMapper objectMapper, 
                          String dlqTopic,
                          KafkaMetrics kafkaMetrics) {
        this(kafkaTemplate, objectMapper, dlqTopic, 3, 1000L, 2.0, kafkaMetrics);
    }

    public DlqErrorHandler(KafkaTemplate<String, String> kafkaTemplate, 
                          ObjectMapper objectMapper, 
                          String dlqTopic,
                          int maxRetries,
                          long initialRetryDelayMs,
                          double retryMultiplier,
                          KafkaMetrics kafkaMetrics) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.dlqTopic = dlqTopic;
        this.maxRetries = maxRetries;
        this.initialRetryDelayMs = initialRetryDelayMs;
        this.retryMultiplier = retryMultiplier;
        this.kafkaMetrics = kafkaMetrics;
    }

    @Override
    public boolean handleOne(Exception exception, 
                            ConsumerRecord<?, ?> record, 
                            Consumer<?, ?> consumer, 
                            MessageListenerContainer container) {
        String messageKey = getMessageKey(record);
        int retryCount = retryCounts.getOrDefault(messageKey, 0);
        
        // Check if this is a JSON deserialization error - don't retry these
        boolean isDeserializationError = isDeserializationError(exception);
        
        if (isDeserializationError) {
            log.error("JSON deserialization error for message from topic: {}, partition: {}, offset: {}. " +
                    "Sending directly to DLQ (no retries for deserialization errors).", 
                    record.topic(), record.partition(), record.offset(), exception);
            sendToDlq(record, exception, 0);
            return true; // Acknowledge and move on
        }
        
        log.error("Error processing Kafka message from topic: {}, partition: {}, offset: {}, retry: {}/{}", 
                record.topic(), record.partition(), record.offset(), retryCount, maxRetries, exception);
        
        // Record processing failure metrics
        if (kafkaMetrics != null) {
            kafkaMetrics.recordProcessingFailure(
                    record.topic(), 
                    extractConsumerGroup(record), 
                    null, 
                    exception.getClass().getSimpleName()
            );
        }

        if (retryCount < maxRetries) {
            // Increment retry count
            retryCounts.put(messageKey, retryCount + 1);
            
            // Calculate exponential backoff delay
            long delayMs = (long) (initialRetryDelayMs * Math.pow(retryMultiplier, retryCount));
            log.info("Will retry message after {}ms (attempt {}/{})", delayMs, retryCount + 1, maxRetries);
            
            // Seek back to current offset to retry
            // The container will pause and retry after the delay
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Retry delay interrupted");
            }
            
            // Return false to seek back and retry
            return false;
        } else {
            // Max retries exceeded, send to DLQ
            log.warn("Max retries ({}) exceeded for message from topic: {}, partition: {}, offset: {}. Sending to DLQ.",
                    maxRetries, record.topic(), record.partition(), record.offset());
            
            sendToDlq(record, exception, retryCount);
            
            // Remove from retry tracking
            retryCounts.remove(messageKey);
            
            // Return true to acknowledge and move on
            return true;
        }
    }

    /**
     * Checks if the exception is a JSON deserialization error.
     * These errors should not be retried as retrying won't fix malformed JSON.
     */
    private boolean isDeserializationError(Exception exception) {
        return exception instanceof JsonProcessingException 
            || exception instanceof JsonMappingException
            || (exception.getCause() != null && 
                (exception.getCause() instanceof JsonProcessingException 
                 || exception.getCause() instanceof JsonMappingException));
    }

    @Override
    public void handleOtherException(Exception exception, 
                                    Consumer<?, ?> consumer, 
                                    MessageListenerContainer container,
                                    boolean batchListener) {
        log.error("Error in Kafka consumer container", exception);
    }

    private String getMessageKey(ConsumerRecord<?, ?> record) {
        return record.topic() + "-" + record.partition() + "-" + record.offset();
    }

    private void sendToDlq(ConsumerRecord<?, ?> record, Exception exception, int retryCount) {
        try {
            DlqMessage dlqMessage = DlqMessage.builder()
                    .originalTopic(record.topic())
                    .originalPartition(record.partition())
                    .originalOffset(record.offset())
                    .originalKey(record.key() != null ? record.key().toString() : null)
                    .originalPayload(record.value() != null ? record.value().toString() : null)
                    .consumerGroup(extractConsumerGroup(record))
                    .errorMessage(exception.getMessage())
                    .errorClass(exception.getClass().getName())
                    .stackTrace(getStackTrace(exception))
                    .retryCount(retryCount)
                    .firstReceivedAt(extractFirstReceivedAt(record))
                    .dlqTimestamp(OffsetDateTime.now(ZoneOffset.UTC))
                    .metadata(Map.of(
                            "timestamp", record.timestamp(),
                            "headers", record.headers() != null ? record.headers().toString() : ""
                    ))
                    .build();

            String dlqPayload = objectMapper.writeValueAsString(dlqMessage);
            
            // Use original key or generate one
            String dlqKey = record.key() != null ? record.key().toString() : 
                           record.topic() + "-" + record.partition() + "-" + record.offset();
            
            kafkaTemplate.send(dlqTopic, dlqKey, dlqPayload);
            log.info("Sent message to DLQ topic: {} with key: {}, retry count: {}", dlqTopic, dlqKey, retryCount);
            
            // Record DLQ metrics
            if (kafkaMetrics != null) {
                kafkaMetrics.recordDlqMessage(record.topic(), extractConsumerGroup(record), retryCount);
            }
        } catch (Exception e) {
            log.error("Failed to send message to DLQ", e);
        }
    }

    private String extractConsumerGroup(ConsumerRecord<?, ?> record) {
        // Try to extract from headers or use default
        if (record.headers() != null) {
            var groupHeader = record.headers().lastHeader("x-consumer-group");
            if (groupHeader != null) {
                return new String(groupHeader.value());
            }
        }
        return "unknown";
    }

    private OffsetDateTime extractFirstReceivedAt(ConsumerRecord<?, ?> record) {
        if (record.headers() != null) {
            var timestampHeader = record.headers().lastHeader("x-first-received-at");
            if (timestampHeader != null) {
                try {
                    return OffsetDateTime.parse(new String(timestampHeader.value())).withOffsetSameInstant(ZoneOffset.UTC);
                } catch (Exception e) {
                    log.debug("Failed to parse first received timestamp", e);
                }
            }
        }
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private String getStackTrace(Exception exception) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }
}

