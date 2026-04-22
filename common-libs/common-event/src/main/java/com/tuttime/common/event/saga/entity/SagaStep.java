package com.tuttime.common.event.saga.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Base entity for tracking individual saga step execution.
 *
 * <p>This is a mapped superclass that provides common fields for saga steps
 * across all services. Each service should extend this class and add the
 * {@code @Entity} and {@code @Table} annotations with their specific schema.
 *
 * <p><b>Step Tracking:</b>
 * <ul>
 *   <li>Each step has a unique order within the saga</li>
 *   <li>Input and output data are stored as JSONB for flexibility</li>
 *   <li>Status tracks step execution state (same as SagaInstance status)</li>
 *   <li>Timestamps track when steps were executed and compensated</li>
 * </ul>
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * @Entity
 * @Table(name = "user_saga_steps", schema = "user_service")
 * public class UserSagaStep extends SagaStep {
 *     // Service-specific fields can be added here
 * }
 * }</pre>
 *
 * @see SagaInstance
 * @see PersistentSagaOrchestrator
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public abstract class SagaStep {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "step_id")
	private Long stepId;

	@Column(name = "saga_id", columnDefinition = "UUID", nullable = false)
	private UUID sagaId;

	@Column(name = "step_name", nullable = false, length = 100)
	private String stepName;

	@Column(name = "step_order", nullable = false)
	private Integer stepOrder;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private SagaInstance.SagaStatus status;

	@Column(name = "input_data", columnDefinition = "JSONB")
	private String inputData; // JSON string

	@Column(name = "output_data", columnDefinition = "JSONB")
	private String outputData; // JSON string

	@Column(name = "error_message", columnDefinition = "TEXT")
	private String errorMessage;

	@Column(name = "executed_at")
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime executedAt;

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
}

