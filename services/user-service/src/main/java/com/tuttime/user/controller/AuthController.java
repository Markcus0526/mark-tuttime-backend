package com.tuttime.user.controller;

import com.tuttime.common.dto.ApiResponse;
import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.exception.CommonMessageKeys;
import com.tuttime.user.dto.*;
import com.tuttime.user.exception.UserServiceMessageKeys;
import com.tuttime.user.service.AntiFraudService;
import com.tuttime.user.service.AuthService;
import com.tuttime.user.service.EmailVerificationService;
import com.tuttime.user.service.SmsVerificationService;
import com.tuttime.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** REST Controller for Authentication: handles signup (SMS verification), signin, token refresh, signout. */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication endpoints")
public class AuthController {

    private final AuthService authService;
    private final SmsVerificationService smsVerificationService;
    private final EmailVerificationService emailVerificationService;
    private final UserService userService;
    private final AntiFraudService antiFraudService;

    /** Sends SMS verification code for signup */
    @PostMapping("/signup/send-sms")
    @Operation(
            summary = "Send SMS verification code",
            description = "Sends a 4-digit SMS code to the provided phone number for signup verification. Rate limit: 3 requests/hour per IP."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "SMS code sent successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid phone number format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<ApiResponse<MessageResponse>> sendSignupSms(
            @Valid @RequestBody PhoneNumberRequest request,
            HttpServletRequest httpRequest) {
        // Normalize phone number (trim whitespace)
        String phoneNumber = request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : null;
        
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
        
        log.info("SMS signup request for phone: {}, deviceType: {} (from header: {}), hasTelegram: {} (from X-Telegram-Account header)", 
                phoneNumber, deviceType, httpRequest != null ? httpRequest.getHeader("X-Device-Type") : "N/A", hasTelegram);
        
        // Check if phone number already registered
        // This check is done here to provide immediate feedback and prevent unnecessary SMS sending
        if (phoneNumber != null && userService.isPhoneRegistered(phoneNumber)) {
            throw new BusinessException(
                    "Phone number is already registered. Please sign in instead.",
                    UserServiceMessageKeys.PHONE_NUMBER_ALREADY_REGISTERED);
        }

        // Check device duplication (only deviceId, not IP)
        // Extract device ID from request header
        String clientDeviceId = extractDeviceIdFromRequest(httpRequest);
        if (clientDeviceId != null && !clientDeviceId.trim().isEmpty()) {
            // Check if device is already registered to another user
            // During signup (userId = 0L), check if device exists for any user
            boolean deviceExists = antiFraudService.isDeviceRegistered(clientDeviceId);
            if (deviceExists) {
                log.warn("Device registration blocked during SMS signup: clientDeviceId={} is already registered to another user", 
                        clientDeviceId);
                throw new BusinessException(
                        "This device is already registered to another account. Please contact support.",
                        UserServiceMessageKeys.DEVICE_ALREADY_REGISTERED);
            }
            log.info("Device validation passed for SMS signup: clientDeviceId={}", clientDeviceId);
        }

        String verificationMethod;
        try {
            verificationMethod = smsVerificationService.sendSignupSms(phoneNumber, deviceType, hasTelegram);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error sending SMS for phone: {}", phoneNumber, e);
            throw new BusinessException(
                    "Failed to send SMS. Please try again later.",
                    UserServiceMessageKeys.FAILED_TO_SEND_SMS);
        }

        // Generate user-friendly message based on verification method
        String message = generateVerificationMessage(verificationMethod);
        
        // Map verification method to verification type
        Integer verificationType = getVerificationType(verificationMethod);

        MessageResponse response = MessageResponse.builder()
                .message(message)
                .verificationMethod(verificationMethod)
                .verificationType(verificationType)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Generates a user-friendly message based on the verification method used.
     * 
     * @param verificationMethod The verification method used
     * @return User-friendly message with instructions
     */
    private String generateVerificationMessage(String verificationMethod) {
        if (verificationMethod == null) {
            return "Verification code sent successfully. Please check your messages.";
        }
        
        switch (verificationMethod) {
            case "Telegram Gateway":
                return "Verification code sent successfully. Please check your Telegram messages for the code.";
            case "Flash Call":
                return "Verification code sent successfully. Please check your missed calls - the code is in the caller ID.";
            case "Green SMS":
            case "Twilio":
                return "Verification code sent successfully. Please check your SMS messages for the code.";
            default:
                return "Verification code sent successfully. Please check your messages for the code.";
        }
    }

    /**
     * Maps verification method to verification type code.
     * 
     * @param verificationMethod The verification method name
     * @return Verification type: 0 = Telegram, 1 = Flash Call, 2 = Others (SMS)
     */
    private Integer getVerificationType(String verificationMethod) {
        if (verificationMethod == null) {
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
                return 2; // Others (SMS)
        }
    }


    /**
     * Verifies SMS code and returns a signup token.
     *
     * <p>This endpoint:
     * <ol>
     *   <li>Validates phone number and SMS code</li>
     *   <li>Checks code in Redis</li>
     *   <li>Verifies code matches</li>
     *   <li>Generates signup token (JWT, 15-minute expiration)</li>
     *   <li>Stores signup token in Redis with phone number</li>
     *   <li>Deletes SMS code from Redis</li>
     * </ol>
     *
     * <p><b>Rate Limit:</b> 10 requests/hour per IP
     *
     * @param request SMS verification request
     * @return Signup token
     */
    @PostMapping("/signup/verify-sms")
    @Operation(
            summary = "Verify SMS code",
            description = "Verifies the SMS code and returns a signup token (JWT, 15-minute expiration) for completing registration. Rate limit: 10 requests/hour per IP."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "SMS code verified successfully, signup token returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired SMS code"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<ApiResponse<SignupTokenResponse>> verifySignupSms(
            @Valid @RequestBody VerifySmsRequest request) {
        log.info("SMS verification request for phone: {}", request.getPhoneNumber());

        String signupToken = smsVerificationService.verifySmsCode(
                request.getPhoneNumber(),
                request.getSmsCode()
        );

        SignupTokenResponse response = SignupTokenResponse.builder()
                .signupToken(signupToken)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Sends an email verification code.
     *
     * <p>This endpoint:
     * <ol>
     *   <li>Validates email format</li>
     *   <li>Checks if email already registered</li>
     *   <li>Generates 4-digit verification code</li>
     *   <li>Stores code in Redis with 10-minute TTL</li>
     *   <li>Sends code via SendGrid email</li>
     * </ol>
     *
     * <p><b>Rate Limit:</b> 3 requests/hour per email
     *
     * @param request Email request
     * @return Success message
     */
    @PostMapping("/email/send")
    @Operation(
            summary = "Send email verification code",
            description = "Sends a 4-digit verification code to the authenticated user's email address. Requires authentication. Rate limit: 3 requests/hour per email."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Email code sent successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid email format or email does not belong to authenticated user"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<ApiResponse<MessageResponse>> sendVerificationEmail(
            @Valid @RequestBody EmailRequest request,
            @AuthenticationPrincipal Long userId) {
        log.info("Email verification request for email: {} (userId: {})", request.getEmail(), userId);

        if (userId == null) {
            throw new com.tuttime.common.exception.AuthenticationException("Authentication required");
        }

        String message;
        try {
            // Pass userId to verify that email belongs to authenticated user
            message = emailVerificationService.sendVerificationEmail(request.getEmail(), userId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error sending verification email for email: {}", request.getEmail(), e);
            throw new BusinessException(
                    "Failed to send verification email. Please try again later.",
                    UserServiceMessageKeys.FAILED_TO_SEND_EMAIL);
        }

        MessageResponse response = MessageResponse.builder()
                .message(message)
                .verificationMethod("Email")
                .verificationType(2) // Email verification type
                .email(request.getEmail()) // Include email address when sent via email
                .sentTo(request.getEmail())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Verifies an email verification code.
     *
     * <p>This endpoint:
     * <ol>
     *   <li>Validates email and code format</li>
     *   <li>Retrieves stored code from Redis</li>
     *   <li>Compares codes</li>
     *   <li>Returns verification result</li>
     * </ol>
     *
     * @param request Email verification request
     * @return Success message
     */
    @PostMapping("/email/verify")
    @Operation(
            summary = "Verify email code",
            description = "Verifies the email verification code sent to the authenticated user's email address. Requires authentication."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Email code verified successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired email code"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    public ResponseEntity<ApiResponse<MessageResponse>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request,
            @AuthenticationPrincipal Long userId) {
        log.info("Email verification request for email: {} (userId: {})", request.getEmail(), userId);

        if (userId == null) {
            throw new com.tuttime.common.exception.AuthenticationException("Authentication required");
        }

        // Verify that the email belongs to the authenticated user
        emailVerificationService.verifyEmailCode(
                request.getEmail(),
                request.getCode(),
                userId
        );

        MessageResponse response = MessageResponse.builder()
                .message("Email verified successfully")
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Completes user signup with signup token.
     *
     * <p>This endpoint:
     * <ol>
     *   <li>Validates signup token</li>
     *   <li>Extracts phone number from token</li>
     *   <li>Validates all required fields</li>
     *   <li>Checks device ID (anti-fraud)</li>
     *   <li>Creates user account</li>
     *   <li>Creates user device record</li>
     *   <li>Generates access token and refresh token</li>
     *   <li>Adds tokens to whitelist</li>
     * </ol>
     *
     * @param request Signup request with user details
     * @return User information and tokens
     */
    @PostMapping("/signup")
    @Operation(
            summary = "Complete user signup",
            description = "Creates a new user account using the signup token from SMS verification. Includes device tracking and anti-fraud checks."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Device registration blocked (anti-fraud)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Phone number already registered")
    })
    public ResponseEntity<ApiResponse<LoginResponse>> signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletRequest httpRequest) {
        log.info("Signup request for phone: {}", request.getPhoneNumber());

        // Validate email is provided (database requires it)
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new BusinessException(
                    "Email is required",
                    UserServiceMessageKeys.EMAIL_REQUIRED);
        }

        // Create user registration request
        UserRegistrationRequest registrationRequest = UserRegistrationRequest.builder()
                .phone(request.getPhoneNumber())
                .password(request.getPassword())
                .email(request.getEmail())
                .username(request.getUsername())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .telegram(request.getTelegramId())
                .country(request.getCountry())
                .city(request.getCity())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .referralCode(request.getReferralCode())
                .interests(request.getInterests())
                .build();

        // Complete signup in a single transaction (will rollback on any error)
        LoginResponse loginResponse = authService.completeSignup(
                registrationRequest,
                request.getSignUpToken(),
                httpRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", loginResponse));
    }

    /**
     * Authenticates a user with phone number and password.
     *
     * <p>This endpoint:
     * <ol>
     *   <li>Validates phone number and password</li>
     *   <li>Checks brute-force protection</li>
     *   <li>Authenticates user</li>
     *   <li>Tracks device usage</li>
     *   <li>Generates tokens with JTI</li>
     *   <li>Adds to whitelist</li>
     *   <li>Resets brute-force attempts</li>
     * </ol>
     *
     * <p><b>Rate Limit:</b> 5 requests/minute per IP
     *
     * @param request Signin request with phone and password
     * @return Authentication tokens
     */
    @PostMapping("/signin")
    @Operation(
            summary = "Sign in user",
            description = "Authenticates user with phone number and password, returns access token (15 minutes) and refresh token (7 days). Rate limit: 5 requests/minute per IP."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Authentication successful, tokens returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Rate limit exceeded or account blocked")
    })
    public ResponseEntity<ApiResponse<AuthTokens>> signin(
            @Valid @RequestBody SigninRequest request,
            HttpServletRequest httpRequest) {
        log.info("Signin request for phone: {}", request.getPhoneNumber());

        // Convert to LoginRequest (using phone to find user)
        // Note: AuthService.login() uses email, so we need to find user by phone first
        // For now, we'll use a phone-based login method
        LoginResponse loginResponse = authService.signinWithPhone(request.getPhoneNumber(), request.getPassword());

        AuthTokens response = AuthTokens.builder()
                .accessToken(loginResponse.getToken())
                .refreshToken(loginResponse.getRefreshToken())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Refreshes access token using refresh token.
     *
     * <p>This endpoint:
     * <ol>
     *   <li>Validates refresh token (signature, expiration, ISS, AUD)</li>
     *   <li>Checks whitelist</li>
     *   <li>Checks blacklist</li>
     *   <li>Extracts user ID</li>
     *   <li>Generates new token pair</li>
     *   <li>Removes old refresh token from whitelist</li>
     *   <li>Adds new tokens to whitelist</li>
     * </ol>
     *
     * <p><b>Authentication:</b> Not required - only refresh token in request body needed
     * <p><b>Rate Limit:</b> 10 requests/minute per user
     *
     * @param request Refresh token request containing refresh token in body
     * @return New access token and refresh token pair
     */
    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Generates a new access token and refresh token pair using a valid refresh token. Old refresh token is invalidated. No Authorization header required - only refresh token in request body. Rate limit: 10 requests/minute per user."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "New tokens generated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid or expired refresh token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<ApiResponse<AuthTokens>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        log.info("Refresh token request received");

        LoginResponse loginResponse = authService.refreshToken(request.getRefreshToken());

        AuthTokens response = AuthTokens.builder()
                .accessToken(loginResponse.getToken())
                .refreshToken(loginResponse.getRefreshToken())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Signs out a user by invalidating their tokens.
     *
     * <p>This endpoint:
     * <ol>
     *   <li>Extracts token from header</li>
     *   <li>Gets JTI from token</li>
     *   <li>Removes from whitelist</li>
     *   <li>Adds to blacklist</li>
     * </ol>
     *
     * <p><b>Authentication:</b> Required
     *
     * @param authHeader Authorization header containing Bearer token
     * @return Success response with message
     */
    @PostMapping("/signout")
    @Operation(
            summary = "Sign out user",
            description = "Invalidates the user's access token and refresh token by removing them from whitelist and adding to blacklist. Authentication required."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sign out successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    public ResponseEntity<ApiResponse<String>> signout(
            @RequestHeader("Authorization") String authHeader) {
        log.info("Signout request received");

        // Extract token from header
        String token = extractTokenFromHeader(authHeader);
        if (token == null) {
            throw new BusinessException(
                    "Invalid authorization header",
                    CommonMessageKeys.INVALID_AUTHORIZATION_HEADER);
        }

        // Get user ID from token
        Long userId = authService.getUserIdFromToken(token);
        if (userId == null) {
            throw new BusinessException(
                    "Invalid token",
                    CommonMessageKeys.INVALID_TOKEN);
        }

        // Logout (removes from whitelist and adds to blacklist)
        authService.logout(token, userId);

        return ResponseEntity.ok(ApiResponse.success("Sign out successful"));
    }

    /**
     * Extracts device ID from request header.
     */
    private String extractDeviceIdFromRequest(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String deviceId = request.getHeader("X-Device-Id");
        return (deviceId != null && !deviceId.trim().isEmpty()) ? deviceId.trim() : null;
    }

    /**
     * Extracts IP address from request.
     */
    private String extractIpAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        // Check X-Forwarded-For header
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            String[] ips = xForwardedFor.split(",");
            return ips[0].trim();
        }

        // Check X-Real-IP header
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp.trim();
        }

        // Fallback to remote address
        return request.getRemoteAddr();
    }

    /**
     * Extracts token from Authorization header.
     */
    private String extractTokenFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }

    /**
     * Checks if a username is available (not already taken).
     * 
     * <p>This endpoint is used by the app to check username availability in real-time
     * as the user types their username during signup.
     * 
     * @param request Username check request
     * @return DuplicationCheckResponse indicating if username is available
     */
    @PostMapping("/check-username")
    @Operation(
            summary = "Check username availability",
            description = "Checks if a username is available (not already taken). Used for real-time validation during signup."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Username availability check completed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request format")
    })
    public ResponseEntity<ApiResponse<DuplicationCheckResponse>> checkUsername(
            @Valid @RequestBody CheckUsernameRequest request) {
        log.info("Username availability check for: {}", request.getUsername());
        
        boolean isTaken = userService.isUsernameTaken(request.getUsername());
        boolean isAvailable = !isTaken;
        
        DuplicationCheckResponse response = DuplicationCheckResponse.builder()
                .isAvailable(isAvailable)
                .value(request.getUsername())
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Checks if an email is available (not already registered).
     * 
     * <p>This endpoint is used by the app to check email availability in real-time
     * as the user types their email during signup.
     * 
     * @param request Email check request
     * @return DuplicationCheckResponse indicating if email is available
     */
    @PostMapping("/check-email")
    @Operation(
            summary = "Check email availability",
            description = "Checks if an email is available (not already registered). Used for real-time validation during signup."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Email availability check completed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request format")
    })
    public ResponseEntity<ApiResponse<DuplicationCheckResponse>> checkEmail(
            @Valid @RequestBody CheckEmailRequest request) {
        log.info("Email availability check for: {}", request.getEmail());
        
        boolean isRegistered = userService.isEmailRegistered(request.getEmail());
        boolean isAvailable = !isRegistered;
        
        DuplicationCheckResponse response = DuplicationCheckResponse.builder()
                .isAvailable(isAvailable)
                .value(request.getEmail())
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}

