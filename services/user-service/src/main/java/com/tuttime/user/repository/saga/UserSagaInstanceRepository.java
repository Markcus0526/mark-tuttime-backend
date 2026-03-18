package com.tuttime.user.repository.saga;

import com.tuttime.common.saga.entity.SagaInstance;
import com.tuttime.user.entity.saga.UserSagaInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSagaInstanceRepository extends JpaRepository<UserSagaInstance, UUID> {
    @Query("SELECT s FROM UserSagaInstance s WHERE s.sagaType = :sagaType AND s.status = :status ORDER BY s.createdAt DESC")
    List<UserSagaInstance> findBySagaTypeAndStatus(@Param("sagaType") String sagaType, @Param("status") SagaInstance.SagaStatus status);

    @Query("SELECT s FROM UserSagaInstance s WHERE s.userId = :userId ORDER BY s.createdAt DESC")
    List<UserSagaInstance> findByUserId(@Param("userId") Long userId);

    @Query("SELECT s FROM UserSagaInstance s WHERE s.correlationId = :correlationId")
    Optional<UserSagaInstance> findByCorrelationId(@Param("correlationId") String correlationId);

    @Query("SELECT s FROM UserSagaInstance s WHERE s.status IN :statuses ORDER BY s.createdAt DESC")
    List<UserSagaInstance> findByStatusIn(@Param("statuses") List<SagaInstance.SagaStatus> statuses);
}

