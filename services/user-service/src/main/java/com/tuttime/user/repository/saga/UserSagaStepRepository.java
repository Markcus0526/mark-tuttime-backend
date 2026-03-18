package com.tuttime.user.repository.saga;

import com.tuttime.user.entity.saga.UserSagaStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserSagaStepRepository extends JpaRepository<UserSagaStep, Long> {
    List<UserSagaStep> findBySagaIdOrderByStepOrder(UUID sagaId);
}

