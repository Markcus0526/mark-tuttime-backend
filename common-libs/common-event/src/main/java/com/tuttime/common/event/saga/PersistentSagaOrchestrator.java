package com.tuttime.common.event.saga;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.event.saga.entity.SagaInstance;
import com.tuttime.common.event.saga.entity.SagaStep;
import com.tuttime.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Persistent Saga Orchestrator
 * Persists saga state to database for recovery and audit
 */
@Slf4j
public abstract class PersistentSagaOrchestrator<T extends SagaInstance, S extends SagaStep> {

    protected final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Execute saga with persistence
     */
    @Transactional
    public <R> R execute(String sagaType, Long userId, String correlationId,
                         List<PersistentSagaStep<R>> steps) throws SagaException {
        UUID sagaId = UUID.randomUUID();
        T sagaInstance = createSagaInstance(sagaType, userId, correlationId, sagaId);
        saveSagaInstance(sagaInstance);

        List<PersistentSagaStep<R>> executedSteps = new ArrayList<>();

        try {
            R result = null;
            int stepOrder = 1;

            for (PersistentSagaStep<R> step : steps) {
                S sagaStep = createSagaStep(sagaId, step.getName(), stepOrder, step.getInputData());
                saveSagaStep(sagaStep);

                log.info("Executing saga step: {} (sagaId: {}, stepOrder: {})", step.getName(), sagaId, stepOrder);

                try {
                    // Update step status to IN_PROGRESS
                    updateStepStatus(sagaStep, SagaInstance.SagaStatus.IN_PROGRESS);

                    // Execute step
                    result = step.execute();

                    // Update step status to COMPLETED
                    updateStepStatus(sagaStep, SagaInstance.SagaStatus.COMPLETED, result);
                    executedSteps.add(step);

                } catch (Exception e) {
                    log.error("Saga step failed: {}", step.getName(), e);
                    updateStepStatus(sagaStep, SagaInstance.SagaStatus.FAILED, e.getMessage());
                    throw e;
                }

                stepOrder++;
            }

            // Update saga status to COMPLETED
            updateSagaStatus(sagaInstance, SagaInstance.SagaStatus.COMPLETED, result);
            log.info("Saga completed successfully: {}", sagaId);
            return result;

        } catch (Exception e) {
            log.error("Saga failed, compensating executed steps: {}", sagaId, e);
            updateSagaStatus(sagaInstance, SagaInstance.SagaStatus.FAILED, e.getMessage());
            compensate(sagaInstance, executedSteps);
            throw new BusinessException("Saga execution failed: " + sagaId, e);
        }
    }

    /**
     * Compensate executed steps in reverse order
     */
    private <R> void compensate(T sagaInstance, List<PersistentSagaStep<R>> executedSteps) {
        updateSagaStatus(sagaInstance, SagaInstance.SagaStatus.COMPENSATING);

        List<S> sagaSteps = getSagaSteps(sagaInstance.getSagaId());

        for (int i = executedSteps.size() - 1; i >= 0; i--) {
            PersistentSagaStep<R> step = executedSteps.get(i);
            S sagaStep = sagaSteps.get(i);

            try {
                log.info("Compensating saga step: {}", step.getName());
                updateStepStatus(sagaStep, SagaInstance.SagaStatus.COMPENSATING);
                step.compensate();
                updateStepStatus(sagaStep, SagaInstance.SagaStatus.COMPENSATED);
            } catch (Exception e) {
                log.error("Error compensating step: {}", step.getName(), e);
                updateStepStatus(sagaStep, SagaInstance.SagaStatus.FAILED, e.getMessage());
                // Continue compensation even if one fails
            }
        }

        updateSagaStatus(sagaInstance, SagaInstance.SagaStatus.COMPENSATED);
    }

    // Abstract methods to be implemented by service-specific orchestrators
    protected abstract T createSagaInstance(String sagaType, Long userId, String correlationId, UUID sagaId);

    protected abstract void saveSagaInstance(T sagaInstance);

    protected abstract void updateSagaStatus(T sagaInstance, SagaInstance.SagaStatus status);

    protected abstract void updateSagaStatus(T sagaInstance, SagaInstance.SagaStatus status, Object result);

    protected abstract void updateSagaStatus(T sagaInstance, SagaInstance.SagaStatus status, String errorMessage);

    protected abstract S createSagaStep(UUID sagaId, String stepName, int stepOrder, Object inputData);

    protected abstract void saveSagaStep(S sagaStep);

    protected abstract void updateStepStatus(S sagaStep, SagaInstance.SagaStatus status);

    protected abstract void updateStepStatus(S sagaStep, SagaInstance.SagaStatus status, Object result);

    protected abstract void updateStepStatus(S sagaStep, SagaInstance.SagaStatus status, String errorMessage);

    protected abstract List<S> getSagaSteps(UUID sagaId);

    /**
     * Persistent Saga Step
     */
    public static class PersistentSagaStep<R> {
        private final String name;
        private final Supplier<R> executeFunction;
        private final Runnable compensateFunction;
        private final Object inputData;

        public PersistentSagaStep(String name, Supplier<R> executeFunction, Runnable compensateFunction, Object inputData) {
            this.name = name;
            this.executeFunction = executeFunction;
            this.compensateFunction = compensateFunction;
            this.inputData = inputData;
        }

        public String getName() {
            return name;
        }

        public R execute() throws Exception {
            return executeFunction.get();
        }

        public void compensate() throws Exception {
            if (compensateFunction != null) {
                compensateFunction.run();
            }
        }

        public Object getInputData() {
            return inputData;
        }
    }

    /**
     * Saga exception
     */
    public static class SagaException extends Exception {
        public SagaException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    protected String toJson(Object obj) {
        try {
            return obj != null ? objectMapper.writeValueAsString(obj) : null;
        } catch (JsonProcessingException e) {
            log.error("Error serializing to JSON", e);
            return null;
        }
    }
}

