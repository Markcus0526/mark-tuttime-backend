package com.tuttime.user.service.sms;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Twilio SMS Provider
 * 
 * <p>Sends SMS verification codes via Twilio Verify API for non-Russian phone numbers.
 * Uses Twilio Verify API which handles code generation and verification automatically.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TwilioSmsProvider implements SmsProvider {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${sms.twilio.verify-api-url:https://verify.twilio.com/v2}")
    private String verifyApiUrl;
    
    @Value("${sms.twilio.account-sid:}")
    private String accountSid;
    
    @Value("${sms.twilio.auth-token:}")
    private String authToken;
    
    @Value("${sms.twilio.service-sid:}")
    private String serviceSid;

    @Value("${sms.twilio.api-url:https://api.twilio.com/2010-04-01}")
    private String apiUrl;
    
    @Override
    public void sendVerificationCode(String phoneNumber, String code) throws Exception {
        if (accountSid == null || accountSid.trim().isEmpty() || 
            authToken == null || authToken.trim().isEmpty()) {
            log.warn("Twilio credentials not configured. Skipping SMS send.");
            throw new IllegalStateException("Twilio credentials not configured");
        }
        
        if (serviceSid == null || serviceSid.trim().isEmpty()) {
            log.warn("Twilio service SID not configured. Skipping SMS send.");
            throw new IllegalStateException("Twilio service SID not configured");
        }
        
        try {
            // Twilio Verify API endpoint: /Services/{ServiceSid}/Verifications
            String url = verifyApiUrl + "/Services/" + serviceSid + "/Verifications";
            
            // Twilio expects form data
            Map<String, String> formData = new HashMap<>();
            formData.put("To", phoneNumber);
            formData.put("Channel", "sms");
            
            // Convert to URL-encoded form data
            StringBuilder formBody = new StringBuilder();
            formData.forEach((key, value) -> {
                if (formBody.length() > 0) {
                    formBody.append("&");
                }
                formBody.append(key).append("=").append(java.net.URLEncoder.encode(value, StandardCharsets.UTF_8));
            });
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            // Twilio uses Basic Auth with Account SID and Auth Token
            headers.setBasicAuth(accountSid, authToken);
            
            HttpEntity<String> request = new HttpEntity<>(formBody.toString(), headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                String status = responseBody != null ? (String) responseBody.get("status") : null;
                log.info("SMS verification code sent via Twilio Verify API to {}: status={}", phoneNumber, status);
            } else {
                handleTwilioError(response, phoneNumber, "send verification");
            }
        } catch (HttpClientErrorException e) {
            handleTwilioHttpError(e, phoneNumber, "send verification");
        } catch (HttpServerErrorException e) {
            log.error("Twilio server error when sending verification to {}: Status: {}, Response: {}", 
                phoneNumber, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("Twilio service is temporarily unavailable. Please try again later.", e);
        } catch (RestClientException e) {
            log.error("Network error when sending verification via Twilio to {}: {}", phoneNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to connect to Twilio service. Please check your network connection.", e);
        } catch (Exception e) {
            log.error("Unexpected error sending SMS verification via Twilio Verify API to {}: {}", phoneNumber, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Verifies a code using Twilio Verify API VerificationCheck endpoint.
     * 
     * @param phoneNumber Phone number in E.164 format
     * @param code Verification code to check
     * @return true if code is valid, false otherwise
     * @throws Exception if verification fails
     */
    public boolean verifyCode(String phoneNumber, String code) throws Exception {
        if (accountSid == null || accountSid.trim().isEmpty() || 
            authToken == null || authToken.trim().isEmpty()) {
            log.warn("Twilio credentials not configured. Cannot verify code.");
            throw new IllegalStateException("Twilio credentials not configured");
        }
        
        if (serviceSid == null || serviceSid.trim().isEmpty()) {
            log.warn("Twilio service SID not configured. Cannot verify code.");
            throw new IllegalStateException("Twilio service SID not configured");
        }
        
        try {
            // Twilio Verify API endpoint: /Services/{ServiceSid}/VerificationCheck
            String url = verifyApiUrl + "/Services/" + serviceSid + "/VerificationCheck";
            
            // Twilio expects form data
            Map<String, String> formData = new HashMap<>();
            formData.put("To", phoneNumber);
            formData.put("Code", code);
            
            // Convert to URL-encoded form data
            StringBuilder formBody = new StringBuilder();
            formData.forEach((key, value) -> {
                if (formBody.length() > 0) {
                    formBody.append("&");
                }
                formBody.append(key).append("=").append(java.net.URLEncoder.encode(value, StandardCharsets.UTF_8));
            });
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            // Twilio uses Basic Auth with Account SID and Auth Token
            headers.setBasicAuth(accountSid, authToken);
            
            HttpEntity<String> request = new HttpEntity<>(formBody.toString(), headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null) {
                    Boolean valid = (Boolean) responseBody.get("valid");
                    String status = (String) responseBody.get("status");
                    log.info("Twilio Verify API verification result for {}: valid={}, status={}", phoneNumber, valid, status);
                    return Boolean.TRUE.equals(valid) && "approved".equals(status);
                }
                return false;
            } else {
                handleTwilioError(response, phoneNumber, "verify code");
                return false;
            }
        } catch (HttpClientErrorException e) {
            // Parse Twilio error for verification-specific errors
            TwilioError twilioError = parseTwilioError(e);
            if (twilioError != null) {
                int errorCode = twilioError.getCode();
                // Handle verification-specific error codes
                if (errorCode == 60202 || errorCode == 60215 || errorCode == 60221 || errorCode == 60227 || 
                    errorCode == 60233 || errorCode == 60239) {
                    // Invalid verification code
                    log.warn("Invalid verification code for {}: Twilio error {} - {}", phoneNumber, errorCode, twilioError.getMessage());
                    return false;
                } else if (errorCode == 60203 || errorCode == 60214 || errorCode == 60220 || errorCode == 60226 || 
                           errorCode == 60232 || errorCode == 60238) {
                    // Expired verification code
                    log.warn("Expired verification code for {}: Twilio error {} - {}", phoneNumber, errorCode, twilioError.getMessage());
                    return false;
                } else if (errorCode == 60206 || errorCode == 60213 || errorCode == 60219 || errorCode == 60225 || 
                           errorCode == 60231 || errorCode == 60237) {
                    // Verification not found
                    log.warn("Verification not found for {}: Twilio error {} - {}", phoneNumber, errorCode, twilioError.getMessage());
                    return false;
                }
            }
            handleTwilioHttpError(e, phoneNumber, "verify code");
            return false;
        } catch (HttpServerErrorException e) {
            log.error("Twilio server error when verifying code for {}: Status: {}, Response: {}", 
                phoneNumber, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("Twilio service is temporarily unavailable. Please try again later.", e);
        } catch (RestClientException e) {
            log.error("Network error when verifying code via Twilio for {}: {}", phoneNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to connect to Twilio service. Please check your network connection.", e);
        } catch (Exception e) {
            log.error("Unexpected error verifying code via Twilio Verify API for {}: {}", phoneNumber, e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public String getProviderName() {
        return "Twilio";
    }

    /**
     * Fetches account balance from Twilio API.
     *
     * @return Balance result or null if not configured or request fails
     */
    public SmsBalanceResult getBalance() {
        if (accountSid == null || accountSid.trim().isEmpty() || authToken == null || authToken.trim().isEmpty()) {
            return null;
        }
        try {
            String url = apiUrl + "/Accounts/" + accountSid + "/Balance.json";
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(accountSid, authToken);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Object balanceObj = body.get("balance");
                Object currencyObj = body.get("currency");
                double balance = 0.0;
                if (balanceObj != null) {
                    try {
                        balance = Double.parseDouble(balanceObj.toString());
                    } catch (NumberFormatException e) {
                        log.debug("Could not parse Twilio balance: {}", balanceObj);
                    }
                }
                String currency = currencyObj != null ? currencyObj.toString() : "USD";
                return SmsBalanceResult.builder()
                        .balance(balance)
                        .currency(currency)
                        .providerName(getProviderName())
                        .build();
            }
        } catch (Exception e) {
            log.debug("Twilio balance check failed: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Handles Twilio error responses from non-2xx status codes.
     * 
     * @param response HTTP response with error
     * @param phoneNumber Phone number for logging
     * @param operation Operation name for logging (e.g., "send verification", "verify code")
     * @throws RuntimeException with appropriate error message
     */
    private void handleTwilioError(ResponseEntity<Map<String, Object>> response, String phoneNumber, String operation) {
        Map<String, Object> responseBody = response.getBody();
        TwilioError twilioError = parseTwilioErrorFromMap(responseBody);
        
        if (twilioError != null) {
            String errorMessage = buildErrorMessage(twilioError, operation);
            log.error("Twilio error when {} for {}: Code: {}, Message: {}", 
                operation, phoneNumber, twilioError.getCode(), twilioError.getMessage());
            throw new RuntimeException(errorMessage);
        } else {
            log.error("Failed to {} via Twilio Verify API for {}. Status: {}, Response: {}", 
                operation, phoneNumber, response.getStatusCode(), responseBody);
            throw new RuntimeException("Failed to " + operation + " via Twilio Verify API: " + response.getStatusCode());
        }
    }
    
    /**
     * Handles Twilio HTTP client/server errors.
     * 
     * @param e HTTP exception
     * @param phoneNumber Phone number for logging
     * @param operation Operation name for logging
     * @throws RuntimeException with appropriate error message
     */
    private void handleTwilioHttpError(HttpClientErrorException e, String phoneNumber, String operation) {
        TwilioError twilioError = parseTwilioError(e);
        
        if (twilioError != null) {
            String errorMessage = buildErrorMessage(twilioError, operation);
            log.error("Twilio HTTP error when {} for {}: Status: {}, Code: {}, Message: {}", 
                operation, phoneNumber, e.getStatusCode(), twilioError.getCode(), twilioError.getMessage());
            throw new RuntimeException(errorMessage, e);
        } else {
            log.error("Twilio HTTP error when {} for {}: Status: {}, Response: {}", 
                operation, phoneNumber, e.getStatusCode(), e.getResponseBodyAsString());
            
            // Handle specific HTTP status codes
            if (e.getStatusCode() != null) {
                int statusCode = e.getStatusCode().value();
                if (statusCode == 401) {
                    throw new RuntimeException("Twilio authentication failed. Please check Account SID and Auth Token configuration.", e);
                } else if (statusCode == 404) {
                    throw new RuntimeException("Twilio service not found. Please check Service SID configuration.", e);
                } else if (statusCode == 429) {
                    throw new RuntimeException("SMS_RATE_LIMIT_EXCEEDED: Too many verification requests. Please wait before requesting another code.", e);
                }
            }
            
            throw new RuntimeException("Failed to " + operation + " via Twilio Verify API: " + e.getStatusCode(), e);
        }
    }
    
    /**
     * Parses Twilio error from HTTP exception response body.
     * 
     * @param e HTTP client error exception
     * @return TwilioError object or null if parsing fails
     */
    private TwilioError parseTwilioError(HttpClientErrorException e) {
        try {
            String responseBody = e.getResponseBodyAsString();
            if (responseBody != null && !responseBody.trim().isEmpty()) {
                Map<String, Object> errorMap = objectMapper.readValue(responseBody, 
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                return parseTwilioErrorFromMap(errorMap);
            }
        } catch (Exception ex) {
            log.debug("Failed to parse Twilio error response: {}", ex.getMessage());
        }
        return null;
    }
    
    /**
     * Parses Twilio error from response map.
     * 
     * @param responseBody Response body map
     * @return TwilioError object or null if parsing fails
     */
    private TwilioError parseTwilioErrorFromMap(Map<String, Object> responseBody) {
        if (responseBody == null) {
            return null;
        }
        
        try {
            Integer code = null;
            String message = null;
            
            // Twilio error format: { "code": 60200, "message": "Invalid parameter", ... }
            Object codeObj = responseBody.get("code");
            if (codeObj instanceof Number) {
                code = ((Number) codeObj).intValue();
            } else if (codeObj instanceof String) {
                try {
                    code = Integer.parseInt((String) codeObj);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
            
            Object messageObj = responseBody.get("message");
            if (messageObj != null) {
                message = messageObj.toString();
            }
            
            if (code != null || message != null) {
                return new TwilioError(code != null ? code : 0, message != null ? message : "Unknown error");
            }
        } catch (Exception e) {
            log.debug("Failed to parse Twilio error from response map: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Builds user-friendly error message from Twilio error.
     * 
     * @param twilioError Twilio error object
     * @param operation Operation name
     * @return User-friendly error message
     */
    private String buildErrorMessage(TwilioError twilioError, String operation) {
        int code = twilioError.getCode();
        String message = twilioError.getMessage();
        
        // Map common Twilio error codes to user-friendly messages
        switch (code) {
            case 60200:
                return "Invalid phone number format. Please check the phone number and try again.";
            case 60201:
                return "Missing required parameter. Please contact support if this issue persists.";
            case 60202:
            case 60215:
            case 60221:
            case 60227:
            case 60233:
            case 60239:
                return "Invalid verification code. Please check the code and try again.";
            case 60203:
            case 60214:
            case 60220:
            case 60226:
            case 60232:
            case 60238:
                return "Verification code has expired. Please request a new code.";
            case 60205:
            case 60363:
                return "Twilio service configuration error. Please contact support.";
            case 60206:
            case 60213:
            case 60219:
            case 60225:
            case 60231:
            case 60237:
                return "Verification not found. Please request a new verification code.";
            case 60246:
                return "SMS_RATE_LIMIT_EXCEEDED: You have exceeded your messaging limits. Please try again later.";
            case 60361:
                return "Twilio account configuration error. Please contact support.";
            case 60327:
            case 60329:
                return "SMS_RATE_LIMIT_EXCEEDED: Too many verification requests. Please wait before requesting another code.";
            default:
                // Return Twilio's message if available, otherwise generic message
                if (message != null && !message.trim().isEmpty()) {
                    return message;
                }
                return "Failed to " + operation + ". Please try again later.";
        }
    }
    
    /**
     * Internal class to represent Twilio error response.
     */
    private static class TwilioError {
        private final int code;
        private final String message;
        
        public TwilioError(int code, String message) {
            this.code = code;
            this.message = message;
        }
        
        public int getCode() {
            return code;
        }
        
        public String getMessage() {
            return message;
        }
    }
}

