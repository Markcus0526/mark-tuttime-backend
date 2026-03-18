package com.aparttoken.common.saga.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Base entity for tracking Saga instance execution state.
 *
 * <p>This is a mapped superclass that provides common fields for saga instances
 * across all services. Each service should extend this class and add the
 * {@code @Entity} and {@code @Table} annotations with their specific schema.
 *
 * <p><b>Saga Lifecycle:</b>
 * <ol>
 *   <li><b>PENDING</b> - Saga created but not started</li>
 *   <li><b>IN_PROGRESS</b> - Saga execution in progress</li>
 *   <li><b>COMPLETED</b> - All steps completed successfully</li>
 *   <li><b>FAILED</b> - Saga failed, compensation may be needed</li>
 *   <li><b>COMPENSATING</b> - Compensation in progress</li>
 *   <li><b>COMPENSATED</b> - Compensation completed</li>
 * </ol>
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * @Entity
 * @Table(name = "user_saga_instances", schema = "user_service")
 * public class UserSagaInstance extends SagaInstance {
 *     // Service-specific fields can be added here
 * }
 * }</pre>
 *
 * @see PersistentSagaOrchestrator
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public abstract class SagaInstance {
    @Id
    @Column(name = "saga_id", columnDefinition = "UUID")
    private UUID sagaId;

    @Column(name = "saga_type", nullable = false, length = 100)
    private String sagaType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SagaStatus status;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "correlation_id", length = 255)
    private String correlationId;

    @Column(name = "input_data", columnDefinition = "JSONB")
    private String inputData; // JSON string - use JSONB in PostgreSQL

    @Column(name = "output_data", columnDefinition = "JSONB")
    private String outputData; // JSON string

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at", nullable = false, updatable = false)
    @CreatedDate
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime completedAt;

    @Column(name = "compensated_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime compensatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @LastModifiedDate
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime updatedAt;

    public enum SagaStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        COMPENSATING,
        COMPENSATED
    }
}

