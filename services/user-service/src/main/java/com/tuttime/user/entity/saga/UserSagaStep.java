package com.tuttime.user.entity.saga;

import com.tuttime.common.saga.entity.SagaInstance;
import com.tuttime.common.saga.entity.SagaStep;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * User Service Saga Step Entity
 *
 * <p>This entity extends the base {@link SagaStep} to represent a single step
 * within a saga orchestration in the User Service. It tracks the execution
 * state and compensation logic for each step in a distributed transaction.
 *
 * <p><b>Inheritance:</b>
 * <p>This class inherits all fields from {@link SagaStep}, including:
 * <ul>
 *   <li>stepId - Unique identifier for the step</li>
 *   <li>sagaId - ID of the parent saga instance</li>
 *   <li>stepName - Name of the step (e.g., "CREATE_USER", "CREATE_TOKEN_BALANCE")</li>
 *   <li>stepOrder - Order of execution within the saga</li>
 *   <li>status - Current status of the step (PENDING, IN_PROGRESS, COMPLETED, etc.)</li>
 *   <li>inputData/outputData - JSON data for step input/output</li>
 *   <li>errorMessage - Error message if step failed</li>
 *   <li>Timestamps - executedAt, compensatedAt, createdAt, updatedAt</li>
 * </ul>
 *
 * <p><b>Relationship:</b>
 * <p>Each saga step belongs to a {@link UserSagaInstance} which orchestrates
 * the overall saga. The relationship is lazy-loaded for performance and is
 * read-only (insertable = false, updatable = false) to prevent accidental
 * modifications through this relationship.
 *
 * <p><b>Use Cases in User Service:</b>
 * <p>Example saga steps for user registration:
 * <ul>
 *   <li>Step 1: CREATE_USER - Create user record</li>
 *   <li>Step 2: CREATE_TOKEN_BALANCE - Create initial token balance (via gRPC)</li>
 *   <li>Step 3: SEND_WELCOME_NOTIFICATION - Send welcome notification (via Kafka)</li>
 * </ul>
 *
 * <p>If any step fails, compensation logic is executed in reverse order:
 * <ul>
 *   <li>Compensate Step 3: Cancel notification</li>
 *   <li>Compensate Step 2: Delete token balance</li>
 *   <li>Compensate Step 1: Delete user record</li>
 * </ul>
 *
 * <p><b>Database Schema:</b>
 * <p>Stored in the "user_service" schema, table "saga_steps".
 * Each service has its own saga_steps table in its own schema.
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * // Create a new saga step
 * UserSagaStep step = new UserSagaStep(
 *     null, // stepId will be auto-generated
 *     sagaId,
 *     "CREATE_USER",
 *     1, // first step
 *     SagaStatus.PENDING,
 *     inputData,
 *     null,
 *     null,
 *     null,
 *     null,
 *     OffsetDateTime.now(ZoneOffset.UTC),
 *     OffsetDateTime.now(ZoneOffset.UTC)
 * );
 * }</pre>
 *
 * @see com.tuttime.common.saga.entity.SagaStep
 * @see UserSagaInstance
 * @see com.tuttime.user.service.saga.UserSagaOrchestrator
 */
@Entity
@Table(name = "saga_steps", schema = "user_service")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UserSagaStep extends SagaStep {

    /**
     * The saga instance this step belongs to.
     *
     * <p>This is a many-to-one relationship where multiple steps belong to
     * a single saga instance. The relationship is:
     * <ul>
     *   <li>Lazy-loaded (FetchType.LAZY) for performance</li>
     *   <li>Read-only (insertable = false, updatable = false) to prevent
     *       accidental modifications through this relationship</li>
     *   <li>Joined on saga_id column</li>
     * </ul>
     *
     * <p><b>Note:</b> The sagaId field inherited from {@link SagaStep} is used
     * for the foreign key relationship. This sagaInstance field provides
     * convenient access to the parent saga instance when needed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saga_id", referencedColumnName = "saga_id", insertable = false, updatable = false)
    private UserSagaInstance sagaInstance;

    /**
     * Constructs a new UserSagaStep with all fields.
     *
     * <p>This constructor initializes all fields inherited from {@link SagaStep}
     * and is useful when creating saga steps programmatically or when loading
     * from the database.
     *
     * @param stepId Unique identifier for the step (null for new steps, auto-generated)
     * @param sagaId ID of the parent saga instance
     * @param stepName Name of the step (e.g., "CREATE_USER", "CREATE_TOKEN_BALANCE")
     * @param stepOrder Order of execution within the saga (1 = first step)
     * @param status Current status of the step
     * @param inputData JSON string containing step input data
     * @param outputData JSON string containing step output data (null until step completes)
     * @param errorMessage Error message if step failed (null if successful)
     * @param executedAt Timestamp when the step was executed (null if not executed)
     * @param compensatedAt Timestamp when compensation completed (null if not compensated)
     * @param createdAt Timestamp when the step was created
     * @param updatedAt Timestamp of last update
     */
    public UserSagaStep(Long stepId, UUID sagaId, String stepName, Integer stepOrder,
                        SagaInstance.SagaStatus status, String inputData, String outputData,
                        String errorMessage, OffsetDateTime executedAt,
                        OffsetDateTime compensatedAt, OffsetDateTime createdAt,
                        OffsetDateTime updatedAt) {
        super(stepId, sagaId, stepName, stepOrder, status, inputData, outputData,
                errorMessage, executedAt, compensatedAt, createdAt, updatedAt);
    }
}

