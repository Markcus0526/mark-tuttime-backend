package com.tuttime.user.repository;

import com.tuttime.user.entity.PhoneVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PhoneVerificationCodeRepository extends JpaRepository<PhoneVerificationCode, UUID> {

	Optional<PhoneVerificationCode> findTopByPhoneNumberAndUsedFalseOrderByCreatedAtDesc(String phoneNumber);

}
