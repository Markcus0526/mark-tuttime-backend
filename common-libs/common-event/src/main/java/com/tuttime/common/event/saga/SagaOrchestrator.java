package com.tuttime.common.event.saga;

import com.tuttime.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Saga Orchestrator for distributed transactions
 * Manages multi-step transactions with compensation
 */
@Slf4j
@Component
public class SagaOrchestrator {

    /**
     * Execute saga steps with compensation on failure
     *
     * @param steps List of saga steps to execute
     * @param <T>   Result type
     * @return Result of last step
     * @throws SagaException if any step fails
     */
    public <T> T execute(List<SagaStep<T>> steps) throws SagaException {
        List<SagaStep<T>> executedSteps = new ArrayList<>();

        try {
            T result = null;
            for (SagaStep<T> step : steps) {
                log.info("Executing saga step: {}", step.getName());
                result = step.execute();
                executedSteps.add(step);
            }
            log.info("Saga completed successfully");
            return result;
        } catch (Exception e) {
            log.error("Saga failed, compensating executed steps", e);
            compensate(executedSteps);
            throw new BusinessException("Saga execution failed", e);
        }
    }

    /**
     * Compensate executed steps in reverse order
     */
    private <T> void compensate(List<SagaStep<T>> executedSteps) {
        for (int i = executedSteps.size() - 1; i >= 0; i--) {
            SagaStep<T> step = executedSteps.get(i);
            try {
                log.info("Compensating saga step: {}", step.getName());
                step.compensate();
            } catch (Exception e) {
                log.error("Error compensating step: {}", step.getName(), e);
                // Continue compensation even if one fails
            }
        }
    }

    /**
     * Saga step interface
     */
    public interface SagaStep<T> {
        String getName();

        T execute() throws Exception;

        void compensate() throws Exception;
    }

    /**
     * Simple saga step implementation
     */
    public static class SimpleSagaStep<T> implements SagaStep<T> {
        private final String name;
        private final Supplier<T> executeFunction;
        private final Runnable compensateFunction;

        public SimpleSagaStep(String name, Supplier<T> executeFunction, Runnable compensateFunction) {
            this.name = name;
            this.executeFunction = executeFunction;
            this.compensateFunction = compensateFunction;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public T execute() throws Exception {
            return executeFunction.get();
        }

        @Override
        public void compensate() throws Exception {
            if (compensateFunction != null) {
                compensateFunction.run();
            }
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
}

