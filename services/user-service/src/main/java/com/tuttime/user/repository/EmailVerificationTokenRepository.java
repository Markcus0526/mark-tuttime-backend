package com.tuttime.user.repository;

import com.tuttime.user.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
	Optional<EmailVerificationToken> findByTokenAndUsedFalse(String token);
}
