package com.tuttime.user.service.sms;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Telegram Gateway SMS Provider
 * 
 * <p>Sends SMS verification codes via Telegram Gateway API.
 * This is used when the user has a Telegram account.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramGatewaySmsProvider implements SmsProvider {
    
    private final RestTemplate restTemplate;
    
    @Value("${sms.telegram.api-url:https://gatewayapi.telegram.org}")
    private String apiUrl;
    
    @Value("${sms.telegram.api-token:}")
    private String apiToken;
    
    @Value("${sms.telegram.rate-limit-delay-ms:3000}")
    private long rateLimitDelayMs; // Delay between API calls to prevent rate limiting (default: 3 seconds)
    
    // Track last API call time to implement rate limiting (thread-safe)
    private final Object rateLimitLock = new Object();
    private volatile long lastApiCallTime = 0;
    
    @Override
    public void sendVerificationCode(String phoneNumber, String code) throws Exception {
        if (apiToken == null || apiToken.trim().isEmpty()) {
            log.warn("Telegram Gateway API token not configured. Skipping SMS send.");
            throw new IllegalStateException("Telegram Gateway API token not configured");
        }
        
        // Rate limiting: Ensure minimum delay between API calls
        enforceRateLimit();
        
        try {
            String url = apiUrl + "/sendVerificationMessage";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiToken);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("phone_number", phoneNumber);
            requestBody.put("code", code);
            requestBody.put("ttl", 300); // 5 minutes TTL
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                
                // According to API documentation, all responses have an "ok" field
                Object okObj = body.get("ok");
                boolean ok = false;
                
                if (okObj instanceof Boolean) {
                    ok = (Boolean) okObj;
                } else if (okObj instanceof String) {
                    ok = Boolean.parseBoolean((String) okObj) || "true".equalsIgnoreCase((String) okObj);
                } else if (okObj != null) {
                    ok = Boolean.parseBoolean(okObj.toString());
                }
                
                if (ok) {
                    Object result = body.get("result");
                    if (result != null) {
                        log.info("SMS sent via Telegram Gateway to {}: {} (request_id: {})", 
                            phoneNumber, code, 
                            result instanceof Map ? ((Map<?, ?>) result).get("request_id") : "N/A");
                    } else {
                        log.info("SMS sent via Telegram Gateway to {}: {}", phoneNumber, code);
                    }
                } else {
                    Object errorObj = body.get("error");
                    String error = errorObj != null ? errorObj.toString() : "Unknown error";
                    log.error("Failed to send SMS via Telegram Gateway. Error: {}", error);
                    
                    // Handle rate limiting (FLOOD_WAIT)
                    if (error.startsWith("FLOOD_WAIT")) {
                        int waitSeconds = parseFloodWaitSeconds(error);
                        throw new TelegramRateLimitException("Telegram Gateway rate limit exceeded. Wait " + waitSeconds + " seconds.", waitSeconds);
                    }
                    
                    throw new RuntimeException("Failed to send SMS via Telegram Gateway: " + error);
                }
            } else {
                log.error("Failed to send SMS via Telegram Gateway. Status: {}, Response: {}", 
                    response.getStatusCode(), response.getBody());
                throw new RuntimeException("Failed to send SMS via Telegram Gateway: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error sending SMS via Telegram Gateway to {}: {}", phoneNumber, e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public String getProviderName() {
        return "Telegram Gateway";
    }
    
    /**
     * Checks if a phone number is registered with Telegram.
     * 
     * <p>According to Telegram Gateway API documentation:
     * - All responses have a Boolean field {@code ok}
     * - If {@code ok} equals {@code true}, the request was successful, and the result can be found in the {@code result} field
     * - If {@code ok} equals {@code false}, there's an {@code error} field explaining the error
     * 
     * <p>For {@code checkSendAbility}:
     * - If the message can be sent, returns a {@code RequestStatus} object (in the {@code result} field)
     * - Otherwise, returns an error (with {@code ok: false} and an {@code error} field)
     * 
     * @param phoneNumber Phone number to check (in E.164 format)
     * @return true if phone number is registered with Telegram, false otherwise
     */
    public boolean checkTelegramAccount(String phoneNumber) {
        if (apiToken == null || apiToken.trim().isEmpty()) {
            log.debug("Telegram Gateway API token not configured. Cannot check Telegram account.");
            return false;
        }
        
        // Rate limiting: Ensure minimum delay between API calls
        enforceRateLimit();
        
        try {
            String url = apiUrl + "/checkSendAbility";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiToken);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("phone_number", phoneNumber);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                log.debug("Telegram Gateway API response for {}: {}", phoneNumber, body);
                
                // According to API documentation, all responses have an "ok" field
                Object okObj = body.get("ok");
                boolean ok = false;
                
                if (okObj instanceof Boolean) {
                    ok = (Boolean) okObj;
                } else if (okObj instanceof String) {
                    ok = Boolean.parseBoolean((String) okObj) || "true".equalsIgnoreCase((String) okObj);
                } else if (okObj != null) {
                    ok = Boolean.parseBoolean(okObj.toString());
                }
                
                if (ok) {
                    // If ok is true, the result field contains a RequestStatus object
                    // This means the phone number can receive messages via Telegram
                    Object result = body.get("result");
                    if (result != null) {
                        log.info("Phone number {} is registered with Telegram (can receive messages)", phoneNumber);
                        return true;
                    } else {
                        log.debug("Phone number {} - API returned ok=true but no result field", phoneNumber);
                    }
                } else {
                    // If ok is false, there's an error field explaining why
                    Object errorObj = body.get("error");
                    String error = errorObj != null ? errorObj.toString() : "Unknown error";
                    log.debug("Phone number {} is not registered with Telegram. Error: {}", phoneNumber, error);
                }
            } else {
                log.debug("Telegram Gateway API returned non-2xx status or empty body for {}: status={}, body={}", 
                    phoneNumber, response.getStatusCode(), response.getBody());
            }
            
            log.debug("Phone number {} is not registered with Telegram", phoneNumber);
            return false;
        } catch (Exception e) {
            log.warn("Error checking Telegram account for {}: {}", phoneNumber, e.getMessage(), e);
            // On error, assume false (not registered) to fall back to other methods
            return false;
        }
    }
    
    /**
     * Enforces rate limiting by ensuring minimum delay between API calls.
     * This helps prevent FLOOD_WAIT errors from Telegram Gateway API.
     * 
     * <p>Uses synchronized block to ensure thread-safe rate limiting across all concurrent requests.
     */
    private void enforceRateLimit() {
        synchronized (rateLimitLock) {
            long currentTime = System.currentTimeMillis();
            long timeSinceLastCall = currentTime - lastApiCallTime;
            
            if (timeSinceLastCall < rateLimitDelayMs) {
                long waitTime = rateLimitDelayMs - timeSinceLastCall;
                try {
                    log.debug("Rate limiting: waiting {} ms before next Telegram Gateway API call (last call was {} ms ago)", 
                        waitTime, timeSinceLastCall);
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Rate limit delay interrupted", e);
                }
            }
            
            lastApiCallTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Parses the wait time in seconds from a FLOOD_WAIT error message.
     * 
     * @param error Error message (e.g., "FLOOD_WAIT_30")
     * @return Wait time in seconds, or 30 if parsing fails
     */
    private int parseFloodWaitSeconds(String error) {
        try {
            // Pattern to match FLOOD_WAIT_<number>
            Pattern pattern = Pattern.compile("FLOOD_WAIT_(\\d+)");
            Matcher matcher = pattern.matcher(error);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        } catch (Exception e) {
            log.warn("Failed to parse FLOOD_WAIT seconds from error: {}", error);
        }
        // Default to 30 seconds if parsing fails
        return 30;
    }
    
    /**
     * Custom exception for Telegram Gateway rate limiting.
     */
    public static class TelegramRateLimitException extends RuntimeException {
        private final int waitSeconds;
        
        public TelegramRateLimitException(String message, int waitSeconds) {
            super(message);
            this.waitSeconds = waitSeconds;
        }
        
        public int getWaitSeconds() {
            return waitSeconds;
        }
    }
}

