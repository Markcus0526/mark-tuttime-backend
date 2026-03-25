package com.tuttime.user.repository;

import com.tuttime.user.entity.AuthProvider;
import com.tuttime.user.entity.UserAuthMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAuthMethodRepository extends JpaRepository<UserAuthMethod, Integer> {
    // Find all auth methods for a specific user (e.g., Google, Email, etc.)
    List<UserAuthMethod> findByUserId(UUID userId);

    // Find a specific auth method for a user (useful for login/verification)
    Optional<UserAuthMethod> findByUserIdAndAuthType(UUID userId, AuthProvider authType);

    // Check if a specific auth type is already registered for a user
    boolean existsByUserIdAndAuthType(UUID userId, AuthProvider authType);
}