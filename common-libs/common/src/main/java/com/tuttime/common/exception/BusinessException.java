package com.tuttime.common.exception;

import lombok.Getter;

/**
 * Business exception for handling business rule violations.
 *
 * <p>This exception supports message keys for internationalization.
 * The message key is used by the frontend to translate error messages.
 *
 * <p><b>Usage:</b>
 * <pre>
 * // Without message key (backward compatible)
 * throw new BusinessException("Phone number is required");
 *
 * // With message key (recommended for i18n)
 * throw new BusinessException("Phone number is required", "PHONE_NUMBER_REQUIRED");
 * </pre>
 */
@Getter
public class BusinessException extends RuntimeException {
	/**
	 * -- GETTER --
	 *  Gets the message key for internationalization.
	 *
	 * @return Message key (e.g., "PHONE_NUMBER_REQUIRED") or null if not provided
	 */
	private final String messageKey;

	/**
	 * Creates a BusinessException with a message only.
	 * Message key will be auto-generated from the message if needed.
	 *
	 * @param message Error message
	 */
	public BusinessException(String message) {
		super(message);
		this.messageKey = null;
	}

	/**
	 * Creates a BusinessException with a message and message key.
	 *
	 * @param message    Error message (default English)
	 * @param messageKey Translation key for internationalization (e.g., "PHONE_NUMBER_REQUIRED")
	 */
	public BusinessException(String message, String messageKey) {
		super(message);
		this.messageKey = messageKey;
	}

	/**
	 * Creates a BusinessException with a message and cause.
	 *
	 * @param message Error message
	 * @param cause   The cause of the exception
	 */
	public BusinessException(String message, Throwable cause) {
		super(message, cause);
		this.messageKey = null;
	}

	/**
	 * Creates a BusinessException with a message, message key, and cause.
	 *
	 * @param message    Error message (default English)
	 * @param messageKey Translation key for internationalization
	 * @param cause      The cause of the exception
	 */
	public BusinessException(String message, String messageKey, Throwable cause) {
		super(message, cause);
		this.messageKey = messageKey;
	}

}
