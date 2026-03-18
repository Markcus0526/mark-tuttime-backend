package com.tuttime.user.entity.saga;

import com.tuttime.common.saga.entity.SagaInstance;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * User Service Saga Instance Entity
 *
 * <p>This entity extends the base {@link SagaInstance} to provide saga orchestration
 * for distributed transactions in the User Service. It tracks the state of
 * multi-step operations that span multiple services.
 *
 * <p><b>Inheritance:</b>
 * <p>This class inherits all fields from {@link SagaInstance}, including:
 * <ul>
 *   <li>sagaId - Unique identifier for the saga instance</li>
 *   <li>sagaType - Type of saga (e.g., "USER_REGISTRATION", "USER_UPDATE")</li>
 *   <li>status - Current status of the saga (PENDING, IN_PROGRESS, COMPLETED, etc.)</li>
 *   <li>userId - ID of the user associated with this saga</li>
 *   <li>correlationId - Correlation ID for tracking across services</li>
 *   <li>inputData/outputData - JSON data for saga input/output</li>
 *   <li>Timestamps - startedAt, completedAt, compensatedAt, createdAt, updatedAt</li>
 * </ul>
 *
 * <p><b>Use Cases in User Service:</b>
 * <ul>
 *   <li>User registration saga (create user, create token balance, send welcome notification)</li>
 *   <li>User update saga (update user, update related services)</li>
 *   <li>User deletion saga (delete user, cleanup related data)</li>
 * </ul>
 *
 * <p><b>Database Schema:</b>
 * <p>Stored in the "user_service" schema, table "saga_instances".
 * Each service has its own saga_instances table in its own schema.
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * // Create a new saga instance
 * UserSagaInstance saga = new UserSagaInstance(
 *     UUID.randomUUID(),
 *     "USER_REGISTRATION",
 *     SagaStatus.PENDING,
 *     userId,
 *     correlationId,
 *     inputData,
 *     null,
 *     null,
 *     OffsetDateTime.now(ZoneOffset.UTC),
 *     null,
 *     null,
 *     OffsetDateTime.now(ZoneOffset.UTC),
 *     OffsetDateTime.now(ZoneOffset.UTC)
 * );
 * }</pre>
 *
 * @see com.tuttime.common.saga.entity.SagaInstance
 * @see com.tuttime.user.service.saga.UserSagaOrchestrator
 */
@Entity
@Table(name = "saga_instances", schema = "user_service")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UserSagaInstance extends SagaInstance {

    /**
     * Constructs a new UserSagaInstance with all fields.
     *
     * <p>This constructor initializes all fields inherited from {@link SagaInstance}
     * and is useful when creating saga instances programmatically or when loading
     * from the database.
     *
     * @param sagaId Unique identifier for the saga instance
     * @param sagaType Type of saga (e.g., "USER_REGISTRATION")
     * @param status Current status of the saga
     * @param userId ID of the user associated with this saga
     * @param correlationId Correlation ID for tracking across services
     * @param inputData JSON string containing saga input data
     * @param outputData JSON string containing saga output data
     * @param errorMessage Error message if saga failed
     * @param startedAt Timestamp when the saga started
     * @param completedAt Timestamp when the saga completed (null if not completed)
     * @param compensatedAt Timestamp when compensation completed (null if not compensated)
     * @param createdAt Timestamp when the saga instance was created
     * @param updatedAt Timestamp of last update
     */
    public UserSagaInstance(UUID sagaId, String sagaType, SagaStatus status, Long userId,
                            String correlationId, String inputData, String outputData,
                            String errorMessage, OffsetDateTime startedAt,
                            OffsetDateTime completedAt, OffsetDateTime compensatedAt,
                            OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        super(sagaId, sagaType, status, userId, correlationId, inputData, outputData,
                errorMessage, startedAt, completedAt, compensatedAt, createdAt, updatedAt);
    }
}

