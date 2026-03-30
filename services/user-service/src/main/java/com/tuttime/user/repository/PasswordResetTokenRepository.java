package com.tuttime.user.repository;

import com.tuttime.user.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
	Optional<PasswordResetToken> findByTokenAndUsedFalse(String token);

	Optional<PasswordResetToken> findByToken(String token);
}
