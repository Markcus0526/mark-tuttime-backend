package com.tuttime.common.exception;

/**
 * Message keys for error internationalization.
 *
 * <p><b>⚠️ DEPRECATED:</b> This class is deprecated. Please use:
 * <ul>
 *   <li>{@link CommonMessageKeys} - for keys shared across multiple services</li>
 *   <li>Service-specific MessageKeys classes (e.g., {@code UserServiceMessageKeys}) - for service-specific keys</li>
 * </ul>
 *
 * <p>This class is kept for backward compatibility only and will be removed in a future version.
 *
 * @see CommonMessageKeys
 * @deprecated Use {@link CommonMessageKeys} and service-specific MessageKeys classes instead.
 */
@Deprecated(since = "1.0", forRemoval = true)
public final class MessageKeys {

	// ========== Authentication - Phone Number ==========
	public static final String PHONE_NUMBER_REQUIRED = "PHONE_NUMBER_REQUIRED";
	public static final String INVALID_PHONE_NUMBER_FORMAT = "INVALID_PHONE_NUMBER_FORMAT";
	public static final String PHONE_NUMBER_ALREADY_REGISTERED = "PHONE_NUMBER_ALREADY_REGISTERED";
	public static final String PHONE_NUMBER_NOT_REGISTERED = "PHONE_NUMBER_NOT_REGISTERED";

	// ========== Authentication - SMS Verification ==========
	public static final String SMS_CODE_REQUIRED = "SMS_CODE_REQUIRED";
	public static final String SMS_CODE_INVALID_OR_EXPIRED = "SMS_CODE_INVALID_OR_EXPIRED";
	public static final String SMS_CODE_VERIFICATION_FAILED = "SMS_CODE_VERIFICATION_FAILED";
	public static final String SMS_SEND_ATTEMPTS_EXCEEDED = "SMS_SEND_ATTEMPTS_EXCEEDED";
	public static final String SMS_SERVICE_UNAVAILABLE = "SMS_SERVICE_UNAVAILABLE";
	public static final String SMS_CODE_NOT_FOUND_OR_EXPIRED = "SMS_CODE_NOT_FOUND_OR_EXPIRED";
	public static final String FAILED_TO_SEND_SMS = "FAILED_TO_SEND_SMS";

	// ========== Authentication - Signup Token ==========
	public static final String SIGNUP_TOKEN_REQUIRED = "SIGNUP_TOKEN_REQUIRED";
	public static final String SIGNUP_TOKEN_INVALID_OR_EXPIRED = "SIGNUP_TOKEN_INVALID_OR_EXPIRED";
	public static final String SIGNUP_TOKEN_MISSING_JTI = "SIGNUP_TOKEN_MISSING_JTI";
	public static final String SIGNUP_TOKEN_EXPIRED_OR_USED = "SIGNUP_TOKEN_EXPIRED_OR_USED";
	public static final String SIGNUP_TOKEN_PHONE_MISMATCH = "SIGNUP_TOKEN_PHONE_MISMATCH";
	public static final String SIGNUP_TOKEN_VALIDATION_SERVICE_UNAVAILABLE = "SIGNUP_TOKEN_VALIDATION_SERVICE_UNAVAILABLE";
	public static final String PHONE_NUMBER_DOES_NOT_MATCH_SIGNUP_TOKEN = "PHONE_NUMBER_DOES_NOT_MATCH_SIGNUP_TOKEN";

	// ========== Authentication - Credentials ==========
	public static final String INVALID_PHONE_NUMBER_OR_PASSWORD = "INVALID_PHONE_NUMBER_OR_PASSWORD";
	public static final String INVALID_EMAIL_OR_PASSWORD = "INVALID_EMAIL_OR_PASSWORD";
	public static final String PASSWORD_REQUIRED = "PASSWORD_REQUIRED";
	public static final String PASSWORD_TOO_SHORT = "PASSWORD_TOO_SHORT";
	public static final String PASSWORD_DOES_NOT_MEET_REQUIREMENTS = "PASSWORD_DOES_NOT_MEET_REQUIREMENTS";

	// ========== Authentication - Refresh Token ==========
	public static final String REFRESH_TOKEN_REQUIRED = "REFRESH_TOKEN_REQUIRED";
	public static final String REFRESH_TOKEN_INVALID_OR_EXPIRED = "REFRESH_TOKEN_INVALID_OR_EXPIRED";
	public static final String REFRESH_TOKEN_MISSING_JTI = "REFRESH_TOKEN_MISSING_JTI";
	public static final String REFRESH_TOKEN_REVOKED = "REFRESH_TOKEN_REVOKED";
	public static final String REFRESH_TOKEN_INVALID_TYPE = "REFRESH_TOKEN_INVALID_TYPE";
	public static final String INVALID_REFRESH_TOKEN = "INVALID_REFRESH_TOKEN";

	// ========== Authentication - User Account ==========
	public static final String USER_ACCOUNT_INACTIVE = "USER_ACCOUNT_INACTIVE";
	public static final String USER_ACCOUNT_BLOCKED = "USER_ACCOUNT_BLOCKED";
	public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
	public static final String EMAIL_REQUIRED = "EMAIL_REQUIRED";
	public static final String EMAIL_ALREADY_REGISTERED = "EMAIL_ALREADY_REGISTERED";
	public static final String USERNAME_REQUIRED = "USERNAME_REQUIRED";
	public static final String USERNAME_ALREADY_TAKEN = "USERNAME_ALREADY_TAKEN";
	public static final String USERNAME_INVALID_FORMAT = "USERNAME_INVALID_FORMAT";

	// ========== Authentication - Signout ==========
	public static final String INVALID_AUTHORIZATION_HEADER = "INVALID_AUTHORIZATION_HEADER";
	public static final String INVALID_TOKEN = "INVALID_TOKEN";
	public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
	public static final String TOKEN_NOT_IN_WHITELIST = "TOKEN_NOT_IN_WHITELIST";
	public static final String TOKEN_BLACKLISTED = "TOKEN_BLACKLISTED";
	public static final String INVALID_TOKEN_ISSUER = "INVALID_TOKEN_ISSUER";
	public static final String INVALID_TOKEN_AUDIENCE = "INVALID_TOKEN_AUDIENCE";
	public static final String USER_ID_NOT_FOUND_IN_TOKEN = "USER_ID_NOT_FOUND_IN_TOKEN";

	// ========== Device & Anti-Fraud ==========
	public static final String DEVICE_FINGERPRINT_REQUIRED = "DEVICE_FINGERPRINT_REQUIRED";
	public static final String DEVICE_ALREADY_REGISTERED = "DEVICE_ALREADY_REGISTERED";
	public static final String DEVICE_LIMIT_REACHED = "DEVICE_LIMIT_REACHED";
	public static final String MULTI_ACCOUNT_DETECTED_IP = "MULTI_ACCOUNT_DETECTED_IP";
	public static final String MULTI_ACCOUNT_DETECTED_DEVICE = "MULTI_ACCOUNT_DETECTED_DEVICE";

	// ========== Rate Limiting & Brute Force ==========
	public static final String TOO_MANY_FAILED_ATTEMPTS = "TOO_MANY_FAILED_ATTEMPTS";
	public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";

	// ========== Service Unavailable ==========
	public static final String SMS_VERIFICATION_SERVICE_UNAVAILABLE = "SMS_VERIFICATION_SERVICE_UNAVAILABLE";
	public static final String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";

	// ========== Generic ==========
	public static final String UNKNOWN_ERROR = "UNKNOWN_ERROR";
	public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";

	private MessageKeys() {
		// Utility class - prevent instantiation
	}
}

