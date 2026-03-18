package com.tuttime.user.service.saga;

import com.tuttime.common.saga.PersistentSagaOrchestrator;
import com.tuttime.common.saga.entity.SagaInstance;
import com.tuttime.user.entity.saga.UserSagaInstance;
import com.tuttime.user.entity.saga.UserSagaStep;
import com.tuttime.user.repository.saga.UserSagaInstanceRepository;
import com.tuttime.user.repository.saga.UserSagaStepRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * User Service specific Saga Orchestrator with persistence
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserSagaOrchestrator extends PersistentSagaOrchestrator<UserSagaInstance, UserSagaStep> {

    private final UserSagaInstanceRepository sagaInstanceRepository;
    private final UserSagaStepRepository sagaStepRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected UserSagaInstance createSagaInstance(String sagaType, Long userId, String correlationId, UUID sagaId) {
        UserSagaInstance instance = new UserSagaInstance();
        instance.setSagaId(sagaId);
        instance.setSagaType(sagaType);
        instance.setStatus(SagaInstance.SagaStatus.PENDING);
        instance.setUserId(userId);
        instance.setCorrelationId(correlationId);
        instance.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC));
        instance.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        instance.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return instance;
    }

    @Override
    @Transactional
    protected void saveSagaInstance(UserSagaInstance sagaInstance) {
        sagaInstanceRepository.save(sagaInstance);
    }

    @Override
    @Transactional
    protected void updateSagaStatus(UserSagaInstance sagaInstance, SagaInstance.SagaStatus status) {
        sagaInstance.setStatus(status);
        sagaInstance.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        if (status == SagaInstance.SagaStatus.COMPLETED) {
            sagaInstance.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        } else if (status == SagaInstance.SagaStatus.COMPENSATED) {
            sagaInstance.setCompensatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        }
        sagaInstanceRepository.save(sagaInstance);
    }

    @Override
    @Transactional
    protected void updateSagaStatus(UserSagaInstance sagaInstance, SagaInstance.SagaStatus status, Object result) {
        sagaInstance.setStatus(status);
        sagaInstance.setOutputData(toJson(result));
        sagaInstance.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        if (status == SagaInstance.SagaStatus.COMPLETED) {
            sagaInstance.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        }
        sagaInstanceRepository.save(sagaInstance);
    }

    @Override
    @Transactional
    protected void updateSagaStatus(UserSagaInstance sagaInstance, SagaInstance.SagaStatus status, String errorMessage) {
        sagaInstance.setStatus(status);
        sagaInstance.setErrorMessage(errorMessage);
        sagaInstance.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        sagaInstanceRepository.save(sagaInstance);
    }

    @Override
    protected UserSagaStep createSagaStep(UUID sagaId, String stepName, int stepOrder, Object inputData) {
        UserSagaStep step = new UserSagaStep();
        step.setSagaId(sagaId);
        step.setStepName(stepName);
        step.setStepOrder(stepOrder);
        step.setStatus(SagaInstance.SagaStatus.PENDING);
        step.setInputData(toJson(inputData));
        step.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        step.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return step;
    }

    @Override
    @Transactional
    protected void saveSagaStep(UserSagaStep sagaStep) {
        sagaStepRepository.save(sagaStep);
    }

    @Override
    @Transactional
    protected void updateStepStatus(UserSagaStep sagaStep, SagaInstance.SagaStatus status) {
        sagaStep.setStatus(status);
        sagaStep.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        if (status == SagaInstance.SagaStatus.COMPLETED || status == SagaInstance.SagaStatus.IN_PROGRESS) {
            sagaStep.setExecutedAt(OffsetDateTime.now(ZoneOffset.UTC));
        } else if (status == SagaInstance.SagaStatus.COMPENSATED) {
            sagaStep.setCompensatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        }
        sagaStepRepository.save(sagaStep);
    }

    @Override
    @Transactional
    protected void updateStepStatus(UserSagaStep sagaStep, SagaInstance.SagaStatus status, Object result) {
        sagaStep.setStatus(status);
        sagaStep.setOutputData(toJson(result));
        sagaStep.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        if (status == SagaInstance.SagaStatus.COMPLETED || status == SagaInstance.SagaStatus.IN_PROGRESS) {
            sagaStep.setExecutedAt(OffsetDateTime.now(ZoneOffset.UTC));
        }
        sagaStepRepository.save(sagaStep);
    }

    @Override
    @Transactional
    protected void updateStepStatus(UserSagaStep sagaStep, SagaInstance.SagaStatus status, String errorMessage) {
        sagaStep.setStatus(status);
        sagaStep.setErrorMessage(errorMessage);
        sagaStep.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        sagaStepRepository.save(sagaStep);
    }

    @Override
    protected List<UserSagaStep> getSagaSteps(UUID sagaId) {
        return sagaStepRepository.findBySagaIdOrderByStepOrder(sagaId);
    }
}

