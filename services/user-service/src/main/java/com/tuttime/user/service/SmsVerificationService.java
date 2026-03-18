package com.tuttime.user.service;

import com.tuttime.common.exception.BusinessException;

/** SMS Verification Service Interface: defines contract for SMS verification operations (code generation, sending, verification). */
public interface SmsVerificationService {
    
    /** Sends verification code via SMS: generates code, stores in Redis with expiration, sends via configured provider, tracks attempts. */
    void sendVerificationCode(String phoneNumber, String code) throws BusinessException;

    /** Verifies code: retrieves from Redis, compares with provided code, validates expiration. */
    boolean verifyCode(String phoneNumber, String code) throws BusinessException;

    /** Generates random 4-digit verification code. */
    String generateCode();

    /** Generates and sends SMS verification code for signup. Workflow: Telegram → Flash Call (Android) → SMS (iOS/Web). Russian numbers use Green SMS, others use Twilio. */
    String sendSignupSms(String phoneNumber, String deviceType, Boolean hasTelegram) throws BusinessException;
    
    /** Generates and sends SMS verification code for signup (backward compatibility). */
    default String sendSignupSms(String phoneNumber) throws BusinessException {
        return sendSignupSms(phoneNumber, null, null);
    }

    /** Verifies SMS code and generates JWT signup token for completing registration. */
    String verifySmsCode(String phoneNumber, String smsCode) throws BusinessException;

    /**
     * Validates a signup token and extracts the phone number.
     * 
     * @param signupToken Signup token to validate
     * @return Phone number associated with the token
     * @throws BusinessException if token is invalid or expired
     */
    String validateSignupToken(String signupToken) throws BusinessException;

    /**
     * Invalidates a signup token after successful signup.
     * 
     * @param signupToken Signup token to invalidate
     */
    void invalidateSignupToken(String signupToken);

    /**
     * Gets the SMS verification code for a phone number (for development/testing).
     * 
     * <p>This method retrieves the stored SMS code from Redis.
     * <b>Note:</b> This is for development/testing only. Should be disabled in production.
     *
     * @param phoneNumber Phone number to get code for
     * @return SMS verification code, or null if not found or expired
     * @throws BusinessException if phone number is invalid or service is unavailable
     */
    String getSmsCode(String phoneNumber) throws BusinessException;
}
