package com.tuttime.user.service.impl;

import com.tuttime.common.exception.BusinessException;
import com.tuttime.user.exception.UserServiceMessageKeys;
import com.tuttime.user.repository.UserRepository;
import com.tuttime.user.service.SmsVerificationService;
import com.tuttime.user.service.sms.*;
import com.tuttime.user.util.JwtTokenUtil;
import com.tuttime.user.util.PhoneNumberUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * SMS Verification Service Implementation
 * 
 * <p>This service handles SMS code generation, storage, and verification for user signup.
 * It also manages signup tokens that are issued after successful SMS verification.
 * 
 * <p><b>Features:</b>
 * <ul>
 *   <li>Generate 4-digit SMS codes</li>
 *   <li>Store codes in Redis with configurable TTL</li>
 *   <li>Verify SMS codes</li>
 *   <li>Track send attempts (max 3 per hour)</li>
 *   <li>Generate signup tokens (JWT, 15-minute expiration)</li>
 *   <li>Store signup tokens in Redis</li>
 * </ul>
 * 
 * <p><b>Redis Key Structure:</b>
 * <ul>
 *   <li>{@code sms:verification:{phoneNumber}} → {code, attempts, expiresAt}</li>
 *   <li>{@code sms:signup:{phoneNumber}} → SMS code (5-minute TTL)</li>
 *   <li>{@code signup:token:{tokenJti}} → phoneNumber (15-minute TTL)</li>
 *   <li>{@code sms:attempts:{phoneNumber}} → attempt count (1-hour TTL)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsVerificationServiceImpl implements SmsVerificationService {

    private final JwtTokenUtil jwtTokenUtil;
    private final UserRepository userRepository;
    private final TelegramGatewaySmsProvider telegramGatewaySmsProvider;
    private final GreenSmsProvider greenSmsProvider;
    private final TwilioSmsProvider twilioSmsProvider;
    private final FlashCallProvider flashCallProvider;

    @Autowired(required = false)
    private SmsDeliveryVerificationAsyncService smsDeliveryVerificationAsyncService;

    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    @Value("${sms.code-length:4}")
    private int codeLength;

    @Value("${sms.code.expiration-minutes:5}")
    private int smsCodeExpirationMinutes;

    @Value("${sms.signup.token.expiration-minutes:15}")
    private int signupTokenExpirationMinutes;

    @Value("${sms.max-attempts-per-hour:3}")
    private int maxAttemptsPerHour;

    @Value("${sms.provider:log}")
    private String smsProvider;

    @Value("${sms.api-key:}")
    private String smsApiKey;

    @Value("${sms.api-secret:}")
    private String smsApiSecret;

    @Value("${sms.from-number:}")
    private String smsFromNumber;

    @Value("${sms.delivery.poll-interval-ms:2000}")
    private long deliveryPollIntervalMs;

    @Value("${sms.delivery.max-poll-attempts:10}")
    private int deliveryMaxPollAttempts;

    @Value("${sms.delivery.max-send-retries:3}")
    private int deliveryMaxSendRetries;

    /** Redis key prefix for SMS verification codes */
    private static final String SMS_VERIFICATION_PREFIX = "sms:verification:";
    
    /** Redis key prefix for SMS signup codes */
    private static final String SMS_SIGNUP_PREFIX = "sms:signup:";
    
    /** Redis key prefix for signup tokens */
    private static final String SIGNUP_TOKEN_PREFIX = "signup:token:";
    
    /** Redis key prefix for SMS send attempts */
    private static final String SMS_ATTEMPTS_PREFIX = "sms:attempts:";
    
    /** Secure random number generator for SMS codes */
    private static final SecureRandom random = new SecureRandom();

    @Override
    public String generateCode() {
        int min = (int) Math.pow(10, codeLength - 1);
        int max = (int) Math.pow(10, codeLength) - 1;
        int code = min + random.nextInt(max - min + 1);
        // Ensure code is exactly codeLength digits by padding with leading zeros if needed
        return String.format("%0" + codeLength + "d", code);
    }

    @Override
    public void sendVerificationCode(String phoneNumber, String code) throws BusinessException {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new BusinessException(
                    "Phone number is required",
                    UserServiceMessageKeys.PHONE_NUMBER_REQUIRED);
        }

        // Check send attempts (rate limiting)
        if (!checkSendAttempts(phoneNumber)) {
            throw new BusinessException(
                    String.format("Maximum SMS send attempts (%d per hour) exceeded. Please try again later.", 
                            maxAttemptsPerHour),
                    UserServiceMessageKeys.SMS_SEND_ATTEMPTS_EXCEEDED);
        }

        // Normalize phone number for consistent provider selection
        String normalizedPhone = PhoneNumberUtil.normalizePhoneNumber(phoneNumber);
        
        // Select provider to determine if we need to generate/store code
        SmsProvider provider = selectSmsProvider(normalizedPhone);
        
        // For Twilio Verify API, codes are generated and managed by Twilio
        // For other providers (Green SMS, etc.), we generate and store codes in Redis
        String verificationCode = null;
        if (!(provider instanceof TwilioSmsProvider)) {
            // Generate code if not provided (for non-Twilio providers)
            verificationCode = (code != null && !code.trim().isEmpty()) ? code : generateCode();

            // Store code in Redis with TTL
            if (redisTemplate != null) {
                String key = SMS_VERIFICATION_PREFIX + normalizedPhone;
                redisTemplate.opsForValue().set(key, verificationCode, smsCodeExpirationMinutes, TimeUnit.MINUTES);
                log.info("SMS code generated and stored for phone: {}, expires in {} minutes", 
                        normalizedPhone, smsCodeExpirationMinutes);
            } else {
                log.warn("Redis not available - SMS code not stored. Code: {} for phone: {}", verificationCode, normalizedPhone);
            }
        } else {
            // For Twilio, code parameter is ignored (Twilio generates it)
            // But we still need to pass something to maintain interface compatibility
            verificationCode = code != null ? code : "0000"; // Dummy value, Twilio will ignore it
            log.info("Using Twilio Verify API - code will be generated by Twilio for phone: {}", normalizedPhone);
        }

        // Send SMS via appropriate provider (for backward compatibility, use SMS provider selection)
        // This method is used for general verification, so we'll use SMS (not Telegram/Flash Call)
        try {
            sendViaProviderWithDeliveryVerification(provider, normalizedPhone, verificationCode);
            log.info("Verification code sent via {} to {}", provider.getProviderName(), normalizedPhone);
        } catch (Exception e) {
            log.error("Error sending verification code to {}: {}", normalizedPhone, e.getMessage(), e);
            
            // For non-Twilio providers, code is stored in Redis even if send fails
            if (!(provider instanceof TwilioSmsProvider)) {
                log.warn("SMS sending failed, but code is stored in Redis. Code: {} for phone: {}", verificationCode, normalizedPhone);
            } else {
                // For Twilio, if send fails, there's no code stored, so we need to throw
                throw new BusinessException(
                        "Failed to send verification code. Please try again later.",
                        UserServiceMessageKeys.FAILED_TO_SEND_SMS);
            }
        }

        // Increment send attempts counter
        incrementSendAttempts(normalizedPhone);
    }

    @Override
    public boolean verifyCode(String phoneNumber, String code) throws BusinessException {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new BusinessException(
                    "Phone number is required",
                    UserServiceMessageKeys.PHONE_NUMBER_REQUIRED);
        }
        if (code == null || code.trim().isEmpty()) {
            throw new BusinessException(
                    "Verification code is required",
                    UserServiceMessageKeys.SMS_CODE_REQUIRED);
        }

        // Normalize phone number for consistent lookup
        String normalizedPhone = PhoneNumberUtil.normalizePhoneNumber(phoneNumber);

        // Strategy: Check Redis first to determine which provider was used
        // - If code exists in Redis → it was sent via Telegram Gateway, Green SMS, or Flash Call
        // - If code NOT in Redis → it was sent via Twilio Verify API (Twilio manages codes)
        
        if (redisTemplate != null) {
            // Get stored code from Redis - try both normalized and original formats for backward compatibility
            String key = SMS_VERIFICATION_PREFIX + normalizedPhone;
            String storedCode = redisTemplate.opsForValue().get(key);
            
            // If not found with normalized format, try original format (for backward compatibility)
            if (storedCode == null && !normalizedPhone.equals(phoneNumber)) {
                String originalKey = SMS_VERIFICATION_PREFIX + phoneNumber;
                storedCode = redisTemplate.opsForValue().get(originalKey);
                if (storedCode != null) {
                    // Migrate to normalized format
                    redisTemplate.opsForValue().set(key, storedCode, smsCodeExpirationMinutes, TimeUnit.MINUTES);
                    redisTemplate.delete(originalKey);
                }
            }

            // If code found in Redis → verify against Redis (code was sent via Telegram/Green SMS/Flash Call)
            if (storedCode != null) {
                // Verify code matches
                if (storedCode.equals(code)) {
                    // Delete code from Redis after successful verification
                    redisTemplate.delete(key);
                    log.info("SMS code verified successfully via Redis for phone: {} (sent via Telegram Gateway/Green SMS/Flash Call)", phoneNumber);
                    return true;
                } else {
                    log.warn("Invalid SMS code provided for phone: {} (verified via Redis)", phoneNumber);
                    throw new BusinessException(
                            "Invalid SMS code. Please try again.",
                            UserServiceMessageKeys.SMS_CODE_VERIFICATION_FAILED);
                }
            }
        }

        // Code not found in Redis → try Twilio Verify API (code was sent via Twilio)
        // Only attempt Twilio if phone number would use Twilio provider
        SmsProvider provider = selectSmsProvider(normalizedPhone);
        if (provider instanceof TwilioSmsProvider) {
            try {
                TwilioSmsProvider twilioProvider = (TwilioSmsProvider) provider;
                boolean isValid = twilioProvider.verifyCode(normalizedPhone, code);
                if (isValid) {
                    log.info("SMS code verified successfully via Twilio Verify API for phone: {}", phoneNumber);
                    // Clean up any Redis entries if they exist (for backward compatibility)
                    if (redisTemplate != null) {
                        String key = SMS_VERIFICATION_PREFIX + normalizedPhone;
                        redisTemplate.delete(key);
                        String signupKey = SMS_SIGNUP_PREFIX + normalizedPhone;
                        redisTemplate.delete(signupKey);
                    }
                    return true;
                } else {
                    log.warn("Invalid SMS code provided for phone: {} (verified via Twilio Verify API)", phoneNumber);
                    throw new BusinessException(
                            "Invalid SMS code. Please try again.",
                            UserServiceMessageKeys.SMS_CODE_VERIFICATION_FAILED);
                }
            } catch (Exception e) {
                log.error("Error verifying code via Twilio Verify API for {}: {}", phoneNumber, e.getMessage(), e);
                
                // Check for Twilio-specific configuration errors
                String errorMessage = e.getMessage();
                if (errorMessage != null) {
                    if (errorMessage.contains("Twilio credentials not configured") || 
                        errorMessage.contains("Twilio service SID not configured") ||
                        errorMessage.contains("Twilio authentication failed") ||
                        errorMessage.contains("Twilio service configuration error") ||
                        errorMessage.contains("Twilio account configuration error")) {
                        throw new BusinessException(
                                "SMS verification service is temporarily unavailable. Please contact support if this issue persists.",
                                UserServiceMessageKeys.SMS_VERIFICATION_SERVICE_UNAVAILABLE);
                    }
                    
                    // Check for invalid/expired code messages from Twilio
                    if (errorMessage.contains("Invalid verification code") || 
                        errorMessage.contains("Invalid phone number format")) {
                        throw new BusinessException(
                                "Invalid SMS code. Please try again.",
                                UserServiceMessageKeys.SMS_CODE_VERIFICATION_FAILED);
                    }
                    
                    if (errorMessage.contains("Verification code has expired") ||
                        errorMessage.contains("Verification not found")) {
                        throw new BusinessException(
                                "SMS code is invalid or has expired. Please request a new code.",
                                UserServiceMessageKeys.SMS_CODE_INVALID_OR_EXPIRED);
                    }
                }
                
                throw new BusinessException(
                        "SMS code verification failed. Please try again.",
                        UserServiceMessageKeys.SMS_CODE_VERIFICATION_FAILED);
            }
        }

        // If Redis is unavailable and not Twilio provider, we can't verify
        if (redisTemplate == null) {
            throw new BusinessException(
                    "SMS verification service is unavailable",
                    UserServiceMessageKeys.SMS_VERIFICATION_SERVICE_UNAVAILABLE);
        }

        // Code not found in Redis and not Twilio provider → code expired or invalid
        log.warn("SMS code not found in Redis and not a Twilio number for phone: {}", phoneNumber);
        throw new BusinessException(
                "SMS code is invalid or has expired. Please request a new code.",
                UserServiceMessageKeys.SMS_CODE_INVALID_OR_EXPIRED);
    }

    @Override
    public String sendSignupSms(String phoneNumber, String deviceType, Boolean hasTelegram) throws BusinessException {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new BusinessException(
                    "Phone number is required",
                    UserServiceMessageKeys.PHONE_NUMBER_REQUIRED);
        }

        // Check send attempts (rate limiting)
        if (!checkSendAttempts(phoneNumber)) {
            throw new BusinessException(
                    String.format("Maximum SMS send attempts (%d per hour) exceeded. Please try again later.", 
                            maxAttemptsPerHour),
                    UserServiceMessageKeys.SMS_SEND_ATTEMPTS_EXCEEDED);
        }

        // Generate verification code (will be overridden for flash calls)
        String verificationCode = generateCode();

        // Use hasTelegram from request parameter (from X-Telegram-Account header)
        // If not provided, default to false (no Telegram account)
        boolean userHasTelegram = hasTelegram != null && hasTelegram;
        
        if (userHasTelegram) {
            log.info("User has Telegram account (from X-Telegram-Account header) for phone: {}", phoneNumber);
        } else {
            log.debug("User does not have Telegram account (or header not set) for phone: {}", phoneNumber);
        }

        // Implement workflow
        SmsProvider provider;
        
        try {
            provider = selectProvider(phoneNumber, deviceType, userHasTelegram);
            
            // Special handling for Flash Call: GreenSMS generates the code
            if (provider instanceof FlashCallProvider) {
                FlashCallProvider flashCallProvider = (FlashCallProvider) provider;
                // GreenSMS generates the code - get it from the response
                verificationCode = flashCallProvider.sendFlashCall(phoneNumber);
                log.info("Flash call sent via Green SMS to {}, code {} generated by GreenSMS", 
                    phoneNumber, verificationCode);
            } else if (provider instanceof TelegramGatewaySmsProvider) {
                // Try Telegram Gateway first if user has Telegram account
                // Store code in Redis BEFORE send to avoid race (user receives SMS before code is stored)
                storeCodeInRedis(phoneNumber, verificationCode);
                try {
                    sendViaProviderWithDeliveryVerification(provider, phoneNumber, verificationCode);
                    log.info("Verification code sent via Telegram Gateway to {}", phoneNumber);
                } catch (Exception telegramException) {
                    // Telegram Gateway failed - fall back to SMS/Flash Call
                    log.warn("Telegram Gateway failed for {}: {}. Falling back to SMS/Flash Call to ensure code delivery.", 
                        phoneNumber, telegramException.getMessage());
                    
                    // Select fallback provider based on device type (ignore Telegram)
                    provider = selectProvider(phoneNumber, deviceType, false);
                    
                    // Handle Flash Call fallback
                    if (provider instanceof FlashCallProvider) {
                        FlashCallProvider flashCallProvider = (FlashCallProvider) provider;
                        verificationCode = flashCallProvider.sendFlashCall(phoneNumber);
                        log.info("Flash call sent via Green SMS (fallback) to {}, code {} generated by GreenSMS", 
                            phoneNumber, verificationCode);
                    } else {
                        // SMS fallback - store code before send to avoid race
                        storeCodeInRedis(phoneNumber, verificationCode);
                        sendViaProviderWithDeliveryVerification(provider, phoneNumber, verificationCode);
                        log.info("Verification code sent via {} (fallback after Telegram Gateway failure) to {}", 
                            provider.getProviderName(), phoneNumber);
                    }
                }
            } else {
                // For SMS providers, use our generated code with delivery verification when supported
                // Store code in Redis BEFORE send to avoid race (user receives SMS before code is stored)
                storeCodeInRedis(phoneNumber, verificationCode);
                sendViaProviderWithDeliveryVerification(provider, phoneNumber, verificationCode);
                log.info("Verification code sent via {} to {}", provider.getProviderName(), phoneNumber);
            }
        } catch (Exception e) {
            log.error("Error sending verification code to {}: {}", phoneNumber, e.getMessage(), e);
            
            // Check if this is a rate limit error (429 from SMS provider or Twilio)
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("SMS_RATE_LIMIT_EXCEEDED")) {
                throw new BusinessException(
                        "Too many verification code requests. Please wait a few minutes before requesting another code.",
                        UserServiceMessageKeys.SMS_RATE_LIMIT_EXCEEDED);
            }
            
            // Check if the cause is a 429 HTTP error
            Throwable cause = e.getCause();
            if (cause instanceof org.springframework.web.client.HttpClientErrorException) {
                org.springframework.web.client.HttpClientErrorException httpException = 
                    (org.springframework.web.client.HttpClientErrorException) cause;
                if (httpException.getStatusCode() != null && httpException.getStatusCode().value() == 429) {
                    throw new BusinessException(
                            "Too many verification code requests. Please wait a few minutes before requesting another code.",
                            UserServiceMessageKeys.SMS_RATE_LIMIT_EXCEEDED);
                }
            }
            
            // Check for Twilio-specific configuration errors
            if (errorMessage != null) {
                if (errorMessage.contains("Twilio credentials not configured") || 
                    errorMessage.contains("Twilio service SID not configured") ||
                    errorMessage.contains("Twilio authentication failed") ||
                    errorMessage.contains("Twilio service configuration error") ||
                    errorMessage.contains("Twilio account configuration error")) {
                    throw new BusinessException(
                            "SMS verification service is temporarily unavailable. Please contact support if this issue persists.",
                            UserServiceMessageKeys.SMS_VERIFICATION_SERVICE_UNAVAILABLE);
                }
            }
            
            // Generic error for other cases
            throw new BusinessException(
                    "Failed to send verification code. Please try again later.",
                    UserServiceMessageKeys.FAILED_TO_SEND_SMS);
        }
        
        // Store the verification code in Redis with TTL
        // For flash calls, this is the code generated by GreenSMS (from response)
        // For Twilio Verify API, codes are managed by Twilio, so we don't store them in Redis
        // For other providers, this is our generated code
        if (!(provider instanceof TwilioSmsProvider)) {
            if (redisTemplate != null) {
                String key = SMS_VERIFICATION_PREFIX + phoneNumber;
                redisTemplate.opsForValue().set(key, verificationCode, smsCodeExpirationMinutes, TimeUnit.MINUTES);
                log.info("Verification code stored in Redis for phone: {}, code: {}, expires in {} minutes", 
                        phoneNumber, verificationCode, smsCodeExpirationMinutes);
            } else {
                log.warn("Redis not available - Verification code not stored. Code: {} for phone: {}", verificationCode, phoneNumber);
            }

            // Also store in signup-specific key for backward compatibility
            if (redisTemplate != null) {
                String signupKey = SMS_SIGNUP_PREFIX + phoneNumber;
                redisTemplate.opsForValue().set(signupKey, verificationCode, smsCodeExpirationMinutes, TimeUnit.MINUTES);
            }
        } else {
            log.info("Verification code sent via Twilio Verify API - code is managed by Twilio, not stored in Redis");
        }

        // Increment send attempts counter
        incrementSendAttempts(phoneNumber);
        
        // Return the verification method used
        return provider.getProviderName();
    }
    
    /**
     * Stores verification code in Redis (verification and signup keys).
     * Call before send to avoid race where user receives SMS before code is stored.
     */
    private void storeCodeInRedis(String phoneNumber, String verificationCode) {
        if (redisTemplate == null) {
            log.warn("Redis not available - Verification code not stored. Code: {} for phone: {}", verificationCode, phoneNumber);
            return;
        }
        String key = SMS_VERIFICATION_PREFIX + phoneNumber;
        redisTemplate.opsForValue().set(key, verificationCode, smsCodeExpirationMinutes, TimeUnit.MINUTES);
        String signupKey = SMS_SIGNUP_PREFIX + phoneNumber;
        redisTemplate.opsForValue().set(signupKey, verificationCode, smsCodeExpirationMinutes, TimeUnit.MINUTES);
        log.debug("Verification code stored in Redis for phone: {}, expires in {} minutes", phoneNumber, smsCodeExpirationMinutes);
    }

    /**
     * Sends verification code via provider. For providers that support delivery verification (e.g. Green SMS),
     * performs sync send and schedules async polling/retry in background. Returns immediately to avoid blocking HTTP thread.
     */
    private void sendViaProviderWithDeliveryVerification(SmsProvider provider, String phoneNumber, String code) throws Exception {
        if (provider instanceof SmsDeliveryVerifiableProvider verifiableProvider) {
            SmsSendResult result = verifiableProvider.sendVerificationCodeWithTracking(phoneNumber, code);
            String requestId = result != null ? result.getRequestId() : null;

            if (requestId != null && !requestId.trim().isEmpty() && smsDeliveryVerificationAsyncService != null) {
                smsDeliveryVerificationAsyncService.verifyAndRetryDeliveryAsync(
                        requestId, phoneNumber, code, verifiableProvider, provider.getProviderName());
            } else if (requestId == null || requestId.trim().isEmpty()) {
                log.debug("Provider {} does not return request_id, skipping delivery verification", provider.getProviderName());
            }
        } else {
            provider.sendVerificationCode(phoneNumber, code);
        }
    }

    /**
     * Selects the appropriate SMS provider based on the workflow.
     * 
     * <p>Workflow:
     * <ol>
     *   <li>If user has Telegram account → try Telegram Gateway first</li>
     *   <li>If Telegram Gateway fails → fall back to SMS/Flash Call (based on device type)</li>
     *   <li>If no Telegram and device is Android → use Flash Call</li>
     *   <li>If no Telegram and device is iOS → send SMS</li>
     *   <li>For SMS: Russian numbers use Green SMS, others use Twilio</li>
     * </ol>
     * 
     * <p><b>Important:</b> If Telegram Gateway is selected but fails, the system automatically
     * falls back to SMS/Flash Call to ensure the verification code is always sent.
     * 
     * @param phoneNumber Phone number
     * @param deviceType Device type: "Android", "iOS", or "Web"
     * @param hasTelegram Whether user has Telegram account
     * @return Selected SMS provider
     */
    private SmsProvider selectProvider(String phoneNumber, String deviceType, boolean hasTelegram) {
        // Step 1: If user has Telegram account → use Telegram Gateway
        if (hasTelegram) {
            log.debug("User has Telegram account, using Telegram Gateway for {}", phoneNumber);
            return telegramGatewaySmsProvider;
        }

        // Step 2: If no Telegram, check device type
        if (deviceType != null) {
            String deviceTypeLower = deviceType.toLowerCase();
            
            // Step 2a: Android → use Flash Call
            if ("android".equals(deviceTypeLower)) {
                log.debug("Android device without Telegram, using Flash Call for {}", phoneNumber);
                return flashCallProvider;
            }
            
            // Step 2b: iOS → send SMS
            if ("ios".equals(deviceTypeLower)) {
                log.debug("iOS device without Telegram, sending SMS for {}", phoneNumber);
                return selectSmsProvider(phoneNumber);
            }
        }

        // Default: send SMS (for Web or unknown device types)
        log.debug("Web or unknown device type, sending SMS for {}", phoneNumber);
        return selectSmsProvider(phoneNumber);
    }
    
    /**
     * Selects SMS provider based on phone number country.
     * Russian numbers use Green SMS, others use Twilio.
     * 
     * @param phoneNumber Phone number
     * @return SMS provider (Green SMS or Twilio)
     */
    private SmsProvider selectSmsProvider(String phoneNumber) {
        // Check if phone number is Russian (+7)
        if (phoneNumber != null && phoneNumber.startsWith("+7")) {
            log.debug("Russian phone number detected, using Green SMS for {}", phoneNumber);
            return greenSmsProvider;
        } else {
            log.debug("Non-Russian phone number, using Twilio for {}", phoneNumber);
            return twilioSmsProvider;
        }
    }
    

    @Override
    public String verifySmsCode(String phoneNumber, String smsCode) throws BusinessException {
        // Normalize phone number for consistent storage and comparison
        String normalizedPhone = PhoneNumberUtil.normalizePhoneNumber(phoneNumber);
        
        // Verify the code (use normalized phone for Redis lookup)
        verifyCode(normalizedPhone, smsCode);

        // Generate signup token (JWT with 15-minute expiration) - use normalized phone
        String signupTokenJti = jwtTokenUtil.generateJti();
        String signupToken = jwtTokenUtil.generateSignupToken(normalizedPhone, signupTokenJti);

        // Store signup token in Redis with normalized phone number
        // Read-after-write verification ensures token is available before returning
        if (redisTemplate != null) {
            String tokenKey = SIGNUP_TOKEN_PREFIX + signupTokenJti;
            redisTemplate.opsForValue().set(tokenKey, normalizedPhone, signupTokenExpirationMinutes, TimeUnit.MINUTES);
            
            // Verify token was stored successfully (read-after-write verification)
            // This prevents issues with Redis replication lag in Kubernetes deployments
            if (!verifySignupTokenExists(tokenKey, 5)) {
                log.warn("Signup token verification failed after write: jti={}, phone={}. " +
                        "Token may not be immediately available due to Redis replication lag. " +
                        "Client may need to retry.", signupTokenJti, normalizedPhone);
                // Continue - the write likely succeeded, just not visible yet due to replication lag
            }
            // Removed debug log for successful token storage to reduce log noise
        }

        // Delete signup-specific key if exists (try both normalized and original formats)
        if (redisTemplate != null) {
            String signupKeyNormalized = SMS_SIGNUP_PREFIX + normalizedPhone;
            redisTemplate.delete(signupKeyNormalized);
            if (!normalizedPhone.equals(phoneNumber)) {
                String signupKeyOriginal = SMS_SIGNUP_PREFIX + phoneNumber;
                redisTemplate.delete(signupKeyOriginal);
            }
        }

        log.info("SMS code verified and signup token generated for phone: {} (normalized: {})", phoneNumber, normalizedPhone);
        return signupToken;
    }
    
    /**
     * Verify that a signup token exists in Redis with retry logic.
     * 
     * <p>This method implements read-after-write verification to handle
     * Redis replication lag in Kubernetes deployments. It retries with
     * exponential backoff to allow time for replication to complete.
     * 
     * @param tokenKey Redis key for the signup token
     * @param maxRetries Maximum number of retry attempts
     * @return true if token exists, false otherwise
     */
    private boolean verifySignupTokenExists(String tokenKey, int maxRetries) {
        if (redisTemplate == null) {
            return false;
        }
        
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                if (Boolean.TRUE.equals(redisTemplate.hasKey(tokenKey))) {
                    if (attempt > 0) {
                        log.debug("Signup token verification succeeded after {} retry attempts: key={}", attempt, tokenKey);
                    }
                    return true;
                }
                
                // If not found and not last attempt, wait before retrying
                if (attempt < maxRetries - 1) {
                    // Exponential backoff: 150ms, 300ms, 600ms, 1200ms, 2400ms (optimized for replication lag)
                    long[] retryDelays = {150, 300, 600, 1200, 2400};
                    long delayMs = attempt < retryDelays.length ? retryDelays[attempt] : retryDelays[retryDelays.length - 1];
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("Signup token verification interrupted: key={}", tokenKey);
                        break;
                    }
                }
            } catch (Exception e) {
                log.warn("Error verifying signup token existence (attempt {}/{}): key={}, error={}", 
                        attempt + 1, maxRetries, tokenKey, e.getMessage());
                // Continue to next retry attempt
            }
        }
        
        return false;
    }

    @Override
    public String validateSignupToken(String signupToken) throws BusinessException {
        if (signupToken == null || signupToken.trim().isEmpty()) {
            throw new BusinessException(
                    "Signup token is required",
                    UserServiceMessageKeys.SIGNUP_TOKEN_REQUIRED);
        }

        if (redisTemplate == null) {
            throw new BusinessException(
                    "Signup token validation service is unavailable",
                    UserServiceMessageKeys.SIGNUP_TOKEN_VALIDATION_SERVICE_UNAVAILABLE);
        }

        // Validate JWT token (signature, expiration, ISS, AUD)
        if (!jwtTokenUtil.validateToken(signupToken)) {
            throw new BusinessException(
                    "Invalid or expired signup token",
                    UserServiceMessageKeys.SIGNUP_TOKEN_INVALID_OR_EXPIRED);
        }

        // Extract JTI from token
        String jti = jwtTokenUtil.getJtiFromToken(signupToken);
        if (jti == null) {
            throw new BusinessException(
                    "Invalid signup token - missing JTI",
                    UserServiceMessageKeys.SIGNUP_TOKEN_MISSING_JTI);
        }

        // Check if token exists in Redis with retry logic to handle replication lag
        // Retry up to 5 times with exponential backoff (150ms, 300ms, 600ms, 1200ms, 2400ms)
        // Optimized delays based on replication lag analysis:
        // - P50: 80ms (covered by 1st retry)
        // - P95: 200ms (covered by 2nd retry)
        // - P99: 400ms (covered by 3rd retry)
        // - P99.9: 540ms (covered by 3rd retry)
        String tokenKey = SIGNUP_TOKEN_PREFIX + jti;
        String phoneNumber = null;
        int maxRetries = 5;
        long[] retryDelays = {150, 300, 600, 1200, 2400}; // Extended for better replication lag handling
        
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            phoneNumber = redisTemplate.opsForValue().get(tokenKey);
            if (phoneNumber != null) {
                break; // Found the token, exit retry loop
            }
            
            // If not found and not the last attempt, wait before retrying
            if (attempt < maxRetries - 1) {
                try {
                    long delayMs = retryDelays[attempt];
                    Thread.sleep(delayMs);
                    log.debug("Signup token not found in Redis (attempt {}/{}), retrying after {}ms due to possible replication lag", 
                            attempt + 1, maxRetries, delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Interrupted while waiting for Redis replication lag retry");
                    break;
                }
            }
        }

        if (phoneNumber == null) {
            log.warn("Signup token not found in Redis after {} attempts (jti: {}) - may be expired, used, or replication lag", 
                    maxRetries, jti);
            throw new BusinessException(
                    "Signup token has expired or been used",
                    UserServiceMessageKeys.SIGNUP_TOKEN_EXPIRED_OR_USED);
        }

        // Extract phone number from token claims as well
        String tokenPhoneNumber = jwtTokenUtil.getClaimFromToken(signupToken, claims -> claims.get("phoneNumber", String.class));
        
        // Normalize both phone numbers for comparison
        String normalizedRedisPhone = PhoneNumberUtil.normalizePhoneNumber(phoneNumber);
        String normalizedTokenPhone = tokenPhoneNumber != null ? PhoneNumberUtil.normalizePhoneNumber(tokenPhoneNumber) : null;
        
        // Verify phone numbers match (after normalization)
        if (normalizedTokenPhone == null || !normalizedTokenPhone.equals(normalizedRedisPhone)) {
            log.warn("Phone number mismatch in signup token validation: Redis={}, Token={}, NormalizedRedis={}, NormalizedToken={}", 
                    phoneNumber, tokenPhoneNumber, normalizedRedisPhone, normalizedTokenPhone);
            throw new BusinessException(
                    "Invalid signup token - phone number mismatch",
                    UserServiceMessageKeys.SIGNUP_TOKEN_PHONE_MISMATCH);
        }

        // Return normalized phone number for consistent comparison in signup flow
        return normalizedRedisPhone;
    }

    @Override
    public void invalidateSignupToken(String signupToken) {
        if (signupToken == null || signupToken.trim().isEmpty() || redisTemplate == null) {
            return;
        }

        try {
            String jti = jwtTokenUtil.getJtiFromToken(signupToken);
            if (jti != null) {
                String tokenKey = SIGNUP_TOKEN_PREFIX + jti;
                redisTemplate.delete(tokenKey);
                log.debug("Signup token invalidated: jti={}", jti);
            }
        } catch (Exception e) {
            log.warn("Error invalidating signup token: {}", e.getMessage());
        }
    }

    /**
     * Checks if SMS can be sent (rate limiting).
     * 
     * @param phoneNumber Phone number to check
     * @return true if SMS can be sent, false if rate limit exceeded
     */
    private boolean checkSendAttempts(String phoneNumber) {
        if (redisTemplate == null) {
            return true; // Allow if Redis is unavailable
        }

        String attemptsKey = SMS_ATTEMPTS_PREFIX + phoneNumber;
        String attemptsStr = redisTemplate.opsForValue().get(attemptsKey);
        
        if (attemptsStr == null) {
            return true; // No previous attempts
        }

        try {
            int attempts = Integer.parseInt(attemptsStr);
            return attempts < maxAttemptsPerHour;
        } catch (NumberFormatException e) {
            log.warn("Invalid attempts count for phone: {}", phoneNumber, e);
            return true; // Allow on error
        }
    }

    /**
     * Increments the send attempts counter for a phone number.
     * 
     * @param phoneNumber Phone number
     */
    private void incrementSendAttempts(String phoneNumber) {
        if (redisTemplate == null) {
            return;
        }

        String attemptsKey = SMS_ATTEMPTS_PREFIX + phoneNumber;
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        
        // Set expiration to 1 hour
        redisTemplate.expire(attemptsKey, 1, TimeUnit.HOURS);
        
        if (attempts != null && attempts >= maxAttemptsPerHour) {
            log.warn("SMS send attempts limit reached for phone: {}, attempts: {}", phoneNumber, attempts);
        }
    }


    @Override
    public String getSmsCode(String phoneNumber) throws BusinessException {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new BusinessException(
                    "Phone number is required",
                    UserServiceMessageKeys.PHONE_NUMBER_REQUIRED);
        }

        if (redisTemplate == null) {
            throw new BusinessException(
                    "SMS verification service is unavailable",
                    UserServiceMessageKeys.SMS_VERIFICATION_SERVICE_UNAVAILABLE);
        }

        // Get stored code from Redis
        String key = SMS_VERIFICATION_PREFIX + phoneNumber;
        String code = redisTemplate.opsForValue().get(key);

        if (code == null) {
            log.warn("SMS code not found or expired for phone: {}", phoneNumber);
            throw new BusinessException(
                    "SMS code not found or has expired. Please request a new code.",
                    UserServiceMessageKeys.SMS_CODE_NOT_FOUND_OR_EXPIRED);
        }

        log.info("SMS code retrieved for phone: {}", phoneNumber);
        return code;
    }
}

