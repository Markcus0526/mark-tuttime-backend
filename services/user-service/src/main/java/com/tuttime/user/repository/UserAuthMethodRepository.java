package com.tuttime.user.repository;

import com.tuttime.user.entity.UserAuthMethod;
import com.tuttime.user.entity.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAuthMethodRepository extends JpaRepository<UserAuthMethod, Integer> {
    // Find all auth methods for a specific user (e.g., Google, Email, etc.)
    List<UserAuthMethod> findByUserId(UUID userId);

    // Find a specific auth method for a user (useful for login/verification)
    Optional<UserAuthMethod> findByUserIdAndAuthType(UUID userId, AuthProvider authType);

    // Check if a specific auth type is already registered for a user
    boolean existsByUserIdAndAuthType(UUID userId, AuthProvider authType);

    Optional<UserAuthMethod> findByAuthTypeAndProviderId(AuthProvider authType, String providerId);
}