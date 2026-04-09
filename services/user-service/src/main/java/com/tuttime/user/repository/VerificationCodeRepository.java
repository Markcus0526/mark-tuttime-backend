package com.tuttime.user.repository;

import com.tuttime.user.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, UUID> {

	Optional<VerificationCode> findTopByTargetAndUsedFalseOrderByCreatedAtDesc(String target);

	Optional<VerificationCode> findTopByCodeHashAndUsedFalseOrderByCreatedAtDesc(String code);

	Optional<VerificationCode> findFirstByTargetAndCodeHashAndUsedFalseAndExpiresAtAfter(
			String target, String code, OffsetDateTime currentTime);

	Optional<VerificationCode> findTopByTargetAndCodeHashAndUsedFalseOrderByCreatedAtDesc(
			String target, String code);
}

