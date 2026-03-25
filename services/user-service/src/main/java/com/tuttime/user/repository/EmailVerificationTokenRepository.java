package com.tuttime.user.repository;

import com.tuttime.user.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
	Optional<EmailVerificationToken> findByTokenAndUsedFalse(String token);
}
