package com.tuttime.user.service;

import com.tuttime.common.exception.BusinessException;

/** Email Verification Service Interface: defines contract for email verification operations (code generation, sending, verification). */
public interface EmailVerificationService {
    
    /** Sends verification code via email: generates code, stores in Redis with expiration, sends via SendGrid, tracks attempts. */
    void sendVerificationCode(String email, String code) throws BusinessException;
    
    /** Verifies code: retrieves from Redis, compares with provided code, validates expiration. */
    boolean verifyCode(String email, String code) throws BusinessException;
    
    /** Generates random 4-digit verification code. */
    String generateCode();
    
    /** Convenience method: generates code and sends via email. */
    String sendVerificationEmail(String email) throws BusinessException;
    
    /** Convenience method: generates code and sends via email. If userId provided, allows verification of own email even if already registered. */
    String sendVerificationEmail(String email, Long userId) throws BusinessException;
    
    /** Verifies email code and updates user's emailVerified status to true. */
    void verifyEmailCode(String email, String code) throws BusinessException;
    
    /** Verifies email code and updates emailVerified status to true. Also verifies email belongs to specified user ID. */
    void verifyEmailCode(String email, String code, Long userId) throws BusinessException;
    
    /**
     * Sends a verification code to the authenticated user's email address.
     * 
     * <p>This method is used for profile email verification where the user is already authenticated.
     * It sends a verification code to the user's registered email address.
     *
     * @param userId User ID of the authenticated user
     * @throws BusinessException if user not found, email not set, or email service unavailable
     */
    void sendProfileVerificationCode(Long userId) throws BusinessException;
}

