package com.tuttime.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Sensitive Data Filter for filtering sensitive information from logs.
 *
 * <p>This filter masks sensitive data in request parameters, headers, and bodies
 * to prevent sensitive information from being logged. It works by wrapping the
 * request and response to intercept data before logging occurs.
 *
 * <p>Filtered sensitive data includes:
 * <ul>
 *   <li>Passwords</li>
 *   <li>Authorization tokens</li>
 *   <li>API keys</li>
 *   <li>Credit card numbers</li>
 *   <li>Social security numbers</li>
 *   <li>Phone numbers (partial masking)</li>
 *   <li>Email addresses (partial masking)</li>
 * </ul>
 */
@Slf4j
@Component
@Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 1)
public class SensitiveDataFilter extends OncePerRequestFilter {

    private static final String MASKED_VALUE = "***MASKED***";
    private static final Set<String> SENSITIVE_HEADERS = new HashSet<>(Arrays.asList(
            "Authorization",
            "X-Api-Key",
            "X-Auth-Token",
            "Cookie",
            "Set-Cookie"
    ));

    private static final Set<String> SENSITIVE_PARAMS = new HashSet<>(Arrays.asList(
            "password",
            "pwd",
            "passwd",
            "secret",
            "token",
            "apiKey",
            "api_key",
            "creditCard",
            "credit_card",
            "cardNumber",
            "card_number",
            "cvv",
            "ssn",
            "socialSecurityNumber"
    ));

    // Patterns for detecting sensitive data in content
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile(
            "\\b(?:\\d[ -]*?){13,19}\\b"
    );

    private static final Pattern SSN_PATTERN = Pattern.compile(
            "\\b\\d{3}-?\\d{2}-?\\d{4}\\b"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Wrap request and response to enable content caching
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            // Copy response content back to original response
            wrappedResponse.copyBodyToResponse();
        }
    }

    /**
     * Masks sensitive header values.
     */
    public static String maskHeaderValue(String headerName, String headerValue) {
        if (headerValue == null) {
            return null;
        }

        if (headerValue.isEmpty()) {
            return headerValue;
        }

        if (SENSITIVE_HEADERS.contains(headerName)) {
            return MASKED_VALUE;
        }

        return headerValue;
    }

    /**
     * Masks sensitive parameter values.
     */
    public static String maskParameterValue(String paramName, String paramValue) {
        if (paramValue == null) {
            return null;
        }

        if (paramValue.isEmpty()) {
            return paramValue;
        }

        String paramNameLower = paramName.toLowerCase();
        for (String sensitiveParam : SENSITIVE_PARAMS) {
            if (sensitiveParam.toLowerCase().equals(paramNameLower)) {
                return MASKED_VALUE;
            }
        }

        return paramValue;
    }

    /**
     * Masks sensitive data in content body.
     */
    public static String maskContentBody(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        // Mask credit card numbers
        content = CREDIT_CARD_PATTERN.matcher(content).replaceAll(MASKED_VALUE);

        // Mask SSN
        content = SSN_PATTERN.matcher(content).replaceAll(MASKED_VALUE);

        // Mask common sensitive fields in JSON
        content = maskJsonFields(content);

        return content;
    }

    /**
     * Masks sensitive fields in JSON content.
     */
    private static String maskJsonFields(String json) {
        for (String sensitiveParam : SENSITIVE_PARAMS) {
            // Match JSON field with various quoting styles
            String pattern = "\"?" + Pattern.quote(sensitiveParam) + "\"?\\s*[:=]\\s*\"?([^\",}\\s]+)\"?";
            json = json.replaceAll("(?i)" + pattern, "\"" + sensitiveParam + "\": \"" + MASKED_VALUE + "\"");
        }
        return json;
    }

    /**
     * Partially masks phone numbers (shows only last 4 digits).
     */
    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return MASKED_VALUE;
        }
        return "***-***-" + phoneNumber.substring(phoneNumber.length() - 4);
    }

    /**
     * Partially masks email addresses (shows only first 2 and last part).
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return MASKED_VALUE;
        }
        String[] parts = email.split("@");
        if (parts[0].length() <= 2) {
            return MASKED_VALUE;
        }
        return parts[0].substring(0, 2) + "***@" + parts[1];
    }
}

