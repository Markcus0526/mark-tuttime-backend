package com.tuttime.user.repository;

import com.tuttime.user.entity.PhoneVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PhoneVerificationCodeRepository extends JpaRepository<PhoneVerificationCode, UUID> {

    Optional<PhoneVerificationCode> findTopByPhoneNumberAndUsedFalseOrderByCreatedAtDesc(String phoneNumber);

}
