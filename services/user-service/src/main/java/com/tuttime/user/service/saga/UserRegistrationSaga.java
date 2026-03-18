package com.tuttime.user.service.saga;

import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.kafka.EventProducer;
import com.tuttime.common.kafka.UserEvent;
import com.tuttime.common.saga.PersistentSagaOrchestrator;
import com.tuttime.user.dto.UserRegistrationRequest;
import com.tuttime.user.dto.UserResponse;
import com.tuttime.user.entity.User;
import com.tuttime.user.mapper.UserMapper;
import com.tuttime.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Saga Orchestration for User Registration
 * Coordinates: User creation → Token balance → AP balance → Notification
 * Uses persistent saga orchestrator for state tracking
 */
@Slf4j
@Service
public class UserRegistrationSaga {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    
    /** Event producer for publishing user events to Kafka (optional - only available when Kafka is enabled) */
    private final EventProducer eventProducer;
    
    private final UserSagaOrchestrator sagaOrchestrator;

    public UserRegistrationSaga(
            UserRepository userRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            @Autowired(required = false) EventProducer eventProducer,
            UserSagaOrchestrator sagaOrchestrator) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.eventProducer = eventProducer;
        this.sagaOrchestrator = sagaOrchestrator;
    }

    @Transactional
    public UserResponse registerUser(UserRegistrationRequest request) {
        log.info("Starting user registration saga for email: {}", request.getEmail());

        String correlationId = UUID.randomUUID().toString();
        List<PersistentSagaOrchestrator.PersistentSagaStep<UserResponse>> steps = new ArrayList<>();

        // Step 1: Create user
        steps.add(new PersistentSagaOrchestrator.PersistentSagaStep<>(
                "create-user",
                () -> {
                    // Check if username is unique
                    String username = request.getUsername().trim();
                    if (userRepository.findByUsername(username).isPresent()) {
                        throw new BusinessException("Username " + username + " is already taken");
                    }

                    User user = User.builder()
                            .email(request.getEmail())
                            .username(username)
                            .passwordHash(passwordEncoder.encode(request.getPassword()))
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .phone(request.getPhone())
                            .telegram(request.getTelegram())
                            .country(request.getCountry())
                            .city(request.getCity())
                            .registrationDate(OffsetDateTime.now(ZoneOffset.UTC))
                            .isVerified(false)
                            .kycCompleted(false)
                            .isActive(true)
                            .isBlocked(false)
                            .build();
                    User savedUser = userRepository.save(user);
                    log.info("Saga Step 1: User created with ID: {}", savedUser.getUserId());
                    return userMapper.toResponse(savedUser);
                },
                () -> {
                    // Compensation: Delete user if created
                    userRepository.findByEmail(request.getEmail())
                            .ifPresent(user -> {
                                userRepository.delete(user);
                                log.info("Saga Compensation: User deleted");
                            });
                },
                request // Input data
        ));

        // Step 2: Publish user created event (other services will create balances)
        steps.add(new PersistentSagaOrchestrator.PersistentSagaStep<>(
                "publish-user-created-event",
                () -> {
                    User user = userRepository.findByEmail(request.getEmail())
                            .orElseThrow(() -> new RuntimeException("User not found"));

                    // Publish event only if Kafka is available
                    if (eventProducer != null) {
                        // Build metadata with interests if provided
                        Map<String, Object> metadata = new HashMap<>();
                        if (request.getInterests() != null && !request.getInterests().isEmpty()) {
                            metadata.put("interests", request.getInterests());
                            log.info("Saga Step 2: Publishing USER_CREATED event with {} interests for userId: {}", 
                                    request.getInterests().size(), user.getUserId());
                        } else {
                            log.info("Saga Step 2: Publishing USER_CREATED event for userId: {}", user.getUserId());
                        }
                        
                        UserEvent event = UserEvent.builder()
                                .eventType(UserEvent.UserEventType.USER_CREATED)
                                .userId(user.getUserId())
                                .email(user.getEmail())
                                .username(user.getUsername())
                                .isVerified(user.getIsVerified())
                                .kycCompleted(user.getKycCompleted())
                                .isActive(user.getIsActive())
                                .isBlocked(user.getIsBlocked())
                                .metadata(metadata.isEmpty() ? null : metadata)
                                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                                .build();

                        eventProducer.publishUserEvent(event);
                        log.debug("Saga Step 2: Published USER_CREATED event: userId={}, hasInterests={}", 
                                user.getUserId(), metadata.containsKey("interests"));
                    } else {
                        log.debug("Saga Step 2: Kafka not available - user created event not published");
                    }
                    return userMapper.toResponse(user);
                },
                null, // No compensation needed (event already published)
                null // No input data
        ));

        try {
            UserResponse result = sagaOrchestrator.execute(
                    "USER_REGISTRATION",
                    null, // userId not available yet
                    correlationId,
                    steps
            );
            log.info("User registration saga completed successfully for user: {}", result.getUserId());
            return result;
        } catch (PersistentSagaOrchestrator.SagaException e) {
            log.error("User registration saga failed", e);
            throw new RuntimeException("User registration failed", e);
        }
    }
}

