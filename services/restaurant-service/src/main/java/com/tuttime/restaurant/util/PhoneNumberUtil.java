package com.tuttime.restaurant.util;

/**
 * Phone Number Utility: provides consistent phone number normalization.
 * 
 * <p>This utility ensures that phone numbers are normalized consistently
 * across the application for storage and comparison purposes.
 * 
 * <p><b>Normalization Rules:</b>
 * <ul>
 *   <li>Removes spaces, dashes, and parentheses</li>
 *   <li>For Russian numbers starting with 7 and length >= 10, adds + prefix</li>
 *   <li>Preserves existing + prefix if present</li>
 * </ul>
 */
public class PhoneNumberUtil {

    /**
     * Normalizes phone number for consistent storage and comparison.
     * Removes spaces, dashes, and ensures consistent format.
     * 
     * <p>For Russian numbers starting with 7 and having at least 10 digits,
     * the + prefix is added if not already present.
     * 
     * @param phoneNumber Phone number to normalize
     * @return Normalized phone number, or null if input is null
     */
    public static String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        // Remove spaces, dashes, parentheses
        String normalized = phoneNumber.replaceAll("[\\s\\-\\(\\)]", "");
        // Ensure + prefix if it's an international number
        if (!normalized.startsWith("+") && normalized.length() > 0 && Character.isDigit(normalized.charAt(0))) {
            // If it starts with a digit and doesn't have +, assume it needs normalization
            // For Russian numbers starting with 7, add +
            if (normalized.startsWith("7") && normalized.length() >= 10) {
                normalized = "+" + normalized;
            }
        }
        return normalized;
    }
}

