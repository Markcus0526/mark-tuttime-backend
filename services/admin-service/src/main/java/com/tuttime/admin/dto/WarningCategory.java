package com.tuttime.admin.dto;

/**
 * Warning Categories
 */
public enum WarningCategory {
	MALICIOUS,          // Malicious activities detected
	SUSPICIOUS,         // Suspicious activities
	BRUTE_FORCE,        // Brute-force attack attempts
	SYSTEM_STABILITY,   // System stability issues
	FRAUD,              // Fraud detection flags
	MULTI_ACCOUNT,      // Multi-account detection
	MULTI_DEVICE,       // Multi-device detection
	UNAUTHORIZED_ACCESS, // Unauthorized access attempts
	RATE_LIMIT_EXCEEDED, // Rate limit violations
	OTHER               // Other types of warnings
}

