package com.tuttime.admin.util;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * Password Validation Utility
 * 
 * <p>Validates password strength according to security requirements:
 * - Minimum 8 characters
 * - At least one uppercase letter
 * - At least one lowercase letter
 * - At least one number
 */
@Slf4j
public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");

    /**
     * Validate password strength
     * 
     * @param password The password to validate
     * @return true if password meets requirements, false otherwise
     */
    public static boolean isValid(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }

        if (password.length() < MIN_LENGTH) {
            log.debug("Password validation failed: too short (min {} characters)", MIN_LENGTH);
            return false;
        }

        if (!UPPERCASE_PATTERN.matcher(password).matches()) {
            log.debug("Password validation failed: missing uppercase letter");
            return false;
        }

        if (!LOWERCASE_PATTERN.matcher(password).matches()) {
            log.debug("Password validation failed: missing lowercase letter");
            return false;
        }

        if (!DIGIT_PATTERN.matcher(password).matches()) {
            log.debug("Password validation failed: missing digit");
            return false;
        }

        return true;
    }

    /**
     * Get validation error message
     * 
     * @param password The password to validate
     * @return Error message if invalid, null if valid
     */
    public static String getValidationError(String password) {
        if (password == null || password.isEmpty()) {
            return "Password is required";
        }

        if (password.length() < MIN_LENGTH) {
            return String.format("Password must be at least %d characters long", MIN_LENGTH);
        }

        if (!UPPERCASE_PATTERN.matcher(password).matches()) {
            return "Password must contain at least one uppercase letter";
        }

        if (!LOWERCASE_PATTERN.matcher(password).matches()) {
            return "Password must contain at least one lowercase letter";
        }

        if (!DIGIT_PATTERN.matcher(password).matches()) {
            return "Password must contain at least one number";
        }

        return null;
    }
}

