package com.tuttime.user.controller;

import com.tuttime.common.dto.ApiResponse;
import com.tuttime.common.exception.BusinessException;
import com.tuttime.user.dto.*;
import com.tuttime.user.service.PasswordResetService;
import com.tuttime.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** REST Controller for Password Management: handles forgot password, email/SMS verification, password reset. Rate limits: 3/hour (forgot), 10/hour (verify), 5/hour (reset). */
@Slf4j
@RestController
@RequestMapping("/api/v1/password")
@RequiredArgsConstructor
@Tag(name = "Password Management", description = "Password reset and management endpoints")
public class PasswordController {

    private final PasswordResetService passwordResetService;

    /** Initiates password reset: validates phone, finds user, checks email verification, sends email code (if verified) or SMS code (if not), returns verification method. Rate limit: 3/hour per IP. */
    @PostMapping("/forgot")
    @Operation(summary = "Request password reset",
            description = "Finds user by phone number and sends verification code via email (if verified) or SMS (if not verified). Returns success with verification method if code was sent, or error if sending failed.")
    public ResponseEntity<ApiResponse<MessageResponse>> forgotPassword(
            @Valid @RequestBody PhoneNumberRequest request,
            HttpServletRequest httpRequest) {
        log.info("Received password reset request for phone: {}", request.getPhoneNumber());

        // Extract device type from HTTP request header (preferred) or request body (fallback)
        String deviceType = null;
        if (httpRequest != null) {
            deviceType = httpRequest.getHeader("X-Device-Type");
        }
        // Fallback to request body if not in header
        if (deviceType == null || deviceType.trim().isEmpty()) {
            deviceType = request.getDeviceType();
        }
        
        // Extract X-Telegram-Account header to determine if user has Telegram account
        Boolean hasTelegram = null;
        if (httpRequest != null) {
            String telegramAccountHeader = httpRequest.getHeader("X-Telegram-Account");
            if (telegramAccountHeader != null && !telegramAccountHeader.trim().isEmpty()) {
                // Parse boolean value (case-insensitive: "true", "True", "TRUE", "1" -> true)
                String normalized = telegramAccountHeader.trim().toLowerCase();
                hasTelegram = normalized.equals("true") || normalized.equals("1");
            }
        }

        // Initiate password reset - will throw BusinessException if it fails
        PasswordResetInitiationResult result = passwordResetService.initiatePasswordReset(
                request.getPhoneNumber(), deviceType, hasTelegram);

        // Check if a verification code was actually sent
        // If sentTo is exactly "email" (generic string, not a real email) and verificationMethod is "Email", 
        // then no code was sent (user doesn't exist case)
        boolean isGenericFallback = result.getSentTo() != null && 
                                    result.getSentTo().equals("email") && 
                                    !result.getSentTo().contains("@") &&
                                    result.getVerificationMethod() != null &&
                                    result.getVerificationMethod().equals("Email");
        
        // If no code was sent (e.g., user doesn't exist), throw exception
        if (isGenericFallback) {
            log.debug("No verification code sent for phone: {} (user may not exist)", request.getPhoneNumber());
            throw new BusinessException("Failed to send verification code. Please try again later.");
        }

        // Check if sentTo is a real email address (contains @) or just a generic string
        boolean isRealEmail = result.getSentTo() != null && result.getSentTo().contains("@");
        
        // Determine verification type and method
        Integer verificationType = null;
        String verificationMethod = result.getVerificationMethod();
        
        if (isRealEmail && verificationMethod != null && verificationMethod.equals("Email")) {
            // Real email was sent
            verificationType = 2; // Email verification type
        } else if (verificationMethod != null) {
            // Real SMS/Telegram/Flash Call was sent
            verificationType = getVerificationType(verificationMethod);
        }

        // Generate user-friendly message based on verification method
        String message = generateVerificationMessage(verificationMethod, result.getSentTo());

        // Build response with verification method information
        MessageResponse.MessageResponseBuilder responseBuilder = MessageResponse.builder()
                .message(message)
                .sentTo(result.getSentTo())
                .retryCount(result.getRetryCount());
        
        // Set verificationMethod and verificationType
        if (verificationMethod != null) {
            responseBuilder.verificationMethod(verificationMethod);
        }
        if (verificationType != null) {
            responseBuilder.verificationType(verificationType);
        }
        
        // Include email address only when verification code was actually sent via email
        if (verificationType != null && verificationType == 2 && isRealEmail) {
            responseBuilder.email(result.getSentTo());
        }

        // Return success response with verification method information
        return ResponseEntity.ok(ApiResponse.success(responseBuilder.build()));
    }

    /**
     * Generates a user-friendly message based on the verification method used and where it was sent.
     * 
     * @param verificationMethod The verification method used
     * @param sentTo Where the verification code was sent to
     * @return User-friendly message with instructions
     */
    private String generateVerificationMessage(String verificationMethod, String sentTo) {
        if (verificationMethod == null || verificationMethod.equals("Email")) {
            if (sentTo != null && !sentTo.equals("email") && sentTo.contains("@")) {
                return String.format("If the phone number is registered, a verification code has been sent to %s.", sentTo);
            }
            return "If the phone number is registered, a verification code has been sent to your email.";
        }
        
        switch (verificationMethod) {
            case "Telegram Gateway":
                return "If the phone number is registered, a verification code has been sent to your Telegram messages.";
            case "Flash Call":
                return "If the phone number is registered, a verification code has been sent via flash call. Please check your missed calls - the code is in the caller ID.";
            case "Green SMS":
            case "Twilio":
                return "If the phone number is registered, a verification code has been sent to your SMS messages.";
            default:
                return "If the phone number is registered, a verification code has been sent.";
        }
    }

    /**
     * Maps verification method to verification type code.
     * 
     * @param verificationMethod The verification method name
     * @return Verification type: 0 = Telegram, 1 = Flash Call, 2 = Others (SMS/Email)
     */
    private Integer getVerificationType(String verificationMethod) {
        if (verificationMethod == null || verificationMethod.equals("Email")) {
            return 2; // Default to others
        }
        
        switch (verificationMethod) {
            case "Telegram Gateway":
                return 0; // Telegram
            case "Flash Call":
                return 1; // Flash Call
            case "Green SMS":
            case "Twilio":
            default:
                return 2; // Others (SMS/Email)
        }
    }

    /**
     * Verifies email or SMS code and generates password reset token.
     *
     * <p>This endpoint:
     * <ol>
     *   <li>Validates phone number and verification code</li>
     *   <li>Finds user by phone number</li>
     *   <li>Checks if user's email is verified</li>
     *   <li>If email is verified: verifies email code</li>
     *   <li>If email is not verified: verifies SMS code</li>
     *   <li>Generates reset token (JWT, 15-minute expiration)</li>
     *   <li>Stores reset token in Redis</li>
     *   <li>Deletes verification code after successful verification</li>
     * </ol>
     *
     * <p><b>Rate Limit:</b> 10 requests/hour per IP
     *
     * @param request Verification request (contains phone number and code)
     * @return Reset token response
     */
    @PostMapping("/verify-code")
    @Operation(summary = "Verify code for password reset",
            description = "Verifies the email or SMS code (depending on user's email verification status) and returns a password reset token.")
    public ResponseEntity<ApiResponse<ResetTokenResponse>> verifyCode(
            @Valid @RequestBody VerifyCodeForPasswordResetRequest request) {
        log.info("Received verification request for password reset, phone: {}", request.getPhoneNumber());

        String resetToken = passwordResetService.verifyCodeAndGenerateResetToken(
                request.getPhoneNumber(),
                request.getCode());

        return ResponseEntity.ok(ApiResponse.success(new ResetTokenResponse(resetToken)));
    }

    /**
     * Verifies SMS code for password reset (dedicated endpoint for SMS verification).
     *
     * <p>This is a convenience endpoint that specifically handles SMS verification
     * for password reset. It's equivalent to calling /verify-code when the user's
     * email is not verified.
     *
     * <p>This endpoint:
     * <ol>
     *   <li>Validates phone number and SMS code</li>
     *   <li>Finds user by phone number</li>
     *   <li>Verifies SMS code</li>
     *   <li>Generates reset token (JWT, 15-minute expiration)</li>
     *   <li>Stores reset token in Redis</li>
     *   <li>Deletes verification code after successful verification</li>
     * </ol>
     *
     * <p><b>Rate Limit:</b> 10 requests/hour per IP
     *
     * @param request SMS verification request (contains phone number and SMS code)
     * @return Reset token response
     */
    @PostMapping("/verify-sms")
    @Operation(summary = "Verify SMS code for password reset",
            description = "Verifies the SMS code and returns a password reset token. Use this endpoint when SMS verification was used for password reset.")
    public ResponseEntity<ApiResponse<ResetTokenResponse>> verifySms(
            @Valid @RequestBody VerifySmsRequest request) {
        log.info("Received SMS verification request for password reset, phone: {}", request.getPhoneNumber());

        // Use the same service method, which will handle SMS verification
        // The service automatically determines whether to verify email or SMS based on user's email verification status
        String resetToken = passwordResetService.verifyCodeAndGenerateResetToken(
                request.getPhoneNumber(),
                request.getSmsCode());

        return ResponseEntity.ok(ApiResponse.success(new ResetTokenResponse(resetToken)));
    }

    /**
     * Resets user password using a valid reset token.
     *
     * <p>This endpoint:
     * <ol>
     *   <li>Validates reset token</li>
     *   <li>Extracts phone number from token</li>
     *   <li>Finds user by phone number</li>
     *   <li>Validates new password strength</li>
     *   <li>Hashes and updates password</li>
     *   <li>Invalidates reset token</li>
     *   <li>Revokes all user tokens (forces re-login)</li>
     * </ol>
     *
     * <p><b>Rate Limit:</b> 5 requests/hour per IP
     * <p><b>Security:</b> All user tokens are revoked after password reset
     *
     * @param request Password reset request
     * @return Success message
     */
    @PostMapping("/reset")
    @Operation(summary = "Reset password",
            description = "Resets the user's password using a valid reset token. All existing tokens are revoked.")
    public ResponseEntity<ApiResponse<MessageResponse>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        log.info("Received password reset request");

        passwordResetService.resetPassword(request.getResetToken(), request.getNewPassword());

        return ResponseEntity.ok(ApiResponse.success(
                MessageResponse.builder()
                        .message("Password has been reset successfully. Please sign in with your new password.")
                        .build()));
    }
}

