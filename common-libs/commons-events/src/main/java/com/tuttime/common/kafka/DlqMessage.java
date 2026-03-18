package com.tuttime.common.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Dead Letter Queue Message
 * 
 * <p>Represents a failed message that has been sent to the DLQ.
 * Contains the original message, error details, and retry information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DlqMessage {
    
    /** Original topic where the message was consumed from */
    private String originalTopic;
    
    /** Original partition */
    private Integer originalPartition;
    
    /** Original offset */
    private Long originalOffset;
    
    /** Original message key */
    private String originalKey;
    
    /** Original message payload */
    private String originalPayload;
    
    /** Consumer group that failed to process the message */
    private String consumerGroup;
    
    /** Error message */
    private String errorMessage;
    
    /** Error class name */
    private String errorClass;
    
    /** Stack trace */
    private String stackTrace;
    
    /** Number of retry attempts made */
    private Integer retryCount;
    
    /** Timestamp when the message was first received */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime firstReceivedAt;
    
    /** Timestamp when the message was sent to DLQ */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime dlqTimestamp;
    
    /** Additional metadata */
    private Map<String, Object> metadata;
}

