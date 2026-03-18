package com.tuttime.user.service;

import com.tuttime.common.exception.BusinessException;
import com.tuttime.user.dto.PasswordResetInitiationResult;

/** Password Reset Service Interface: defines contract for password reset operations (email verification, reset token generation, password update). */
public interface PasswordResetService {
    
    /** Initiates password reset: validates phone, finds user, checks email verification, sends email/SMS code, stores in Redis. Always returns success to prevent enumeration. */
    PasswordResetInitiationResult initiatePasswordReset(String phoneNumber, String deviceType, Boolean hasTelegram) throws BusinessException;

    /** Verifies email/SMS code and generates password reset token (JWT, 15-minute expiration). Stores in Redis, deletes code after verification. */
    String verifyCodeAndGenerateResetToken(String phoneNumber, String code) throws BusinessException;

    /** Validates reset token and extracts phone number. */
    String validateResetToken(String resetToken) throws BusinessException;

    /** Resets password: validates token, extracts phone, finds user, validates password strength, hashes/updates password, invalidates token, revokes all user tokens. */
    void resetPassword(String resetToken, String newPassword) throws BusinessException;

    /** Invalidates reset token after successful password reset. */
    void invalidateResetToken(String resetToken);
}

