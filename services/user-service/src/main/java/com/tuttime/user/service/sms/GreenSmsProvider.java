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

/**
 * Green SMS Provider
 * 
 * <p>Sends SMS verification codes via Green SMS API for Russian phone numbers.
 * Uses JWT token for authentication.
 * Supports delivery status polling via /sms/status endpoint.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GreenSmsProvider implements SmsDeliveryVerifiableProvider {
    
    private final RestTemplate restTemplate;
    
    @Value("${sms.green.api-url:}")
    private String apiUrl;
    
    @Value("${sms.green.jwt-token:}")
    private String jwtToken;
    
    @Value("${sms.green.status-endpoint:/sms/status}")
    private String statusEndpoint;
    
    @Override
    public void sendVerificationCode(String phoneNumber, String code) throws Exception {
        sendVerificationCodeWithTracking(phoneNumber, code);
    }
    
    @Override
    public SmsSendResult sendVerificationCodeWithTracking(String phoneNumber, String code) throws Exception {
        if (jwtToken == null || jwtToken.trim().isEmpty()) {
            log.warn("Green SMS JWT token not configured. Skipping SMS send.");
            throw new IllegalStateException("Green SMS JWT token not configured");
        }
        
        try {
            // Validate configuration
            if (apiUrl == null || apiUrl.trim().isEmpty()) {
                throw new IllegalStateException("Green SMS API URL not configured. Set SMS_GREEN_API_URL environment variable.");
            }
            
            // Remove + from phone number for Green SMS API
            String normalizedPhone = phoneNumber.startsWith("+") ? phoneNumber.substring(1) : phoneNumber;
            
            // Green SMS API endpoint: /sms/send (based on working example)
            String url = apiUrl + "/sms/send";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + jwtToken);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("to", normalizedPhone);
            requestBody.put("txt", "BOOST-X.PRO: Your verification code is: " + code);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            log.debug("Sending SMS via Green SMS to URL: {}, phone: {}", url, normalizedPhone);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                String requestId = null;
                if (responseBody != null && responseBody.containsKey("request_id")) {
                    requestId = String.valueOf(responseBody.get("request_id"));
                    log.info("SMS sent via Green SMS to {}: {} (request_id: {})", 
                        phoneNumber, code, requestId);
                } else {
                    log.info("SMS sent via Green SMS to {}: {}", phoneNumber, code);
                }
                return SmsSendResult.builder()
                        .requestId(requestId)
                        .providerName(getProviderName())
                        .build();
            } else {
                log.error("Failed to send SMS via Green SMS. Status: {}, Response: {}", 
                    response.getStatusCode(), response.getBody());
                throw new RuntimeException("Failed to send SMS via Green SMS: " + response.getStatusCode());
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Handle 403 and other HTTP errors specifically
            if (e.getStatusCode() != null && e.getStatusCode().value() == 403) {
                log.error("Green SMS API returned 403 Forbidden. Possible issues: " +
                    "1) Invalid JWT token, 2) Wrong API endpoint, 3) Token expired. " +
                    "URL: {}, Phone: {}, Response: {}", apiUrl + "/sms/send", phoneNumber, e.getResponseBodyAsString());
                throw new RuntimeException("Green SMS authentication failed (403 Forbidden). Please check JWT token and API endpoint configuration.");
            }
            // Handle 429 Too Many Requests (rate limit)
            if (e.getStatusCode() != null && e.getStatusCode().value() == 429) {
                log.warn("Green SMS API returned 429 Too Many Requests for SMS to {}. " +
                    "Rate limits exceeded. Response: {}", 
                    phoneNumber, e.getResponseBodyAsString());
                // Throw a specific exception that can be caught and handled with a rate limit message
                RuntimeException rateLimitException = new RuntimeException("SMS_RATE_LIMIT_EXCEEDED: Too many SMS requests to this number. Please wait before requesting another code.");
                rateLimitException.initCause(e);
                throw rateLimitException;
            }
            log.error("HTTP error sending SMS via Green SMS to {}: Status: {}, Response: {}", 
                phoneNumber, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw e;
        } catch (Exception e) {
            log.error("Error sending SMS via Green SMS to {}: {}", phoneNumber, e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public DeliveryStatus checkDeliveryStatus(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            return DeliveryStatus.UNKNOWN;
        }
        if (apiUrl == null || apiUrl.trim().isEmpty() || jwtToken == null || jwtToken.trim().isEmpty()) {
            return DeliveryStatus.UNKNOWN;
        }
        if (statusEndpoint == null || statusEndpoint.trim().isEmpty()) {
            return DeliveryStatus.UNKNOWN;
        }
        
        try {
            String url = apiUrl + statusEndpoint + "?request_id=" + requestId.trim();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtToken);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                // Green SMS status: "delivered", "sent", "failed", "pending", etc.
                Object statusObj = body.get("status");
                if (statusObj == null) {
                    statusObj = body.get("state");
                }
                String status = statusObj != null ? statusObj.toString().toLowerCase() : "";
                if (status.contains("delivered") || status.contains("complete") || status.equals("1")) {
                    return DeliveryStatus.DELIVERED;
                }
                if (status.contains("failed") || status.contains("error") || status.equals("2") || status.equals("-1")) {
                    return DeliveryStatus.FAILED;
                }
                return DeliveryStatus.PENDING;
            }
        } catch (Exception e) {
            log.debug("Green SMS status check failed for request_id {}: {}", requestId, e.getMessage());
        }
        return DeliveryStatus.UNKNOWN;
    }
    
    @Override
    public String getProviderName() {
        return "Green SMS";
    }

    /**
     * Fetches account balance from Green SMS API.
     *
     * @return Balance result or null if not configured or request fails
     */
    public SmsBalanceResult getBalance() {
        if (apiUrl == null || apiUrl.trim().isEmpty() || jwtToken == null || jwtToken.trim().isEmpty()) {
            return null;
        }
        try {
            String url = apiUrl + "/account/balance";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtToken);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object balanceObj = response.getBody().get("balance");
                double balance = 0.0;
                if (balanceObj instanceof Number) {
                    balance = ((Number) balanceObj).doubleValue();
                } else if (balanceObj != null) {
                    try {
                        balance = Double.parseDouble(balanceObj.toString());
                    } catch (NumberFormatException e) {
                        log.debug("Could not parse Green SMS balance: {}", balanceObj);
                    }
                }
                return SmsBalanceResult.builder()
                        .balance(balance)
                        .currency("RUB")
                        .providerName(getProviderName())
                        .build();
            }
        } catch (Exception e) {
            log.debug("Green SMS balance check failed: {}", e.getMessage());
        }
        return null;
    }
}

