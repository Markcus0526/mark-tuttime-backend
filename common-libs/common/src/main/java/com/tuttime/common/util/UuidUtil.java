package com.tuttime.common.util;

import java.util.Optional;
import java.util.UUID;

public class UuidUtil {

	/**
	 * Safely converts a String to a UUID.
	 * Returns null if the input is null, empty, or malformed.
	 */
	public static UUID toUuid(String uuidStr) {
		if (uuidStr == null || uuidStr.isBlank()) {
			return null;
		}
		try {
			return UUID.fromString(uuidStr.trim());
		} catch (IllegalArgumentException e) {
			// Log error if necessary
			return null;
		}
	}

	/**
	 * Converts a UUID to a String.
	 * Returns null if the input object is null.
	 */
	public static String fromUuid(UUID uuid) {
		return (uuid != null) ? uuid.toString() : null;
	}

	/**
	 * Advanced: Returns an Optional for cleaner functional handling.
	 */
	public static Optional<UUID> toOptionalUuid(String uuidStr) {
		return Optional.ofNullable(toUuid(uuidStr));
	}
}
