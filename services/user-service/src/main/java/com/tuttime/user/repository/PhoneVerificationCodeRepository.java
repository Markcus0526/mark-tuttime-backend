package com.tuttime.user.repository;

import com.tuttime.user.entity.PhoneVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PhoneVerificationCodeRepository extends JpaRepository<PhoneVerificationCode, UUID> {

	Optional<PhoneVerificationCode> findTopByPhoneNumberAndUsedFalseOrderByCreatedAtDesc(String phoneNumber);

	Optional<PhoneVerificationCode> findTopByCodeAndUsedFalseOrderByCreatedAtDesc(String code);

	Optional<PhoneVerificationCode> findFirstByPhoneNumberAndCodeAndUsedFalseAndExpiresAtAfter(
			String phoneNumber, String code, OffsetDateTime currentTime);

	Optional<PhoneVerificationCode> findTopByPhoneNumberAndCodeAndUsedFalseOrderByCreatedAtDesc(
			String phoneNumber, String code);
}

