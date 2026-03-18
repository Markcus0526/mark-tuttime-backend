package com.tuttime.common.exception;

/**
 * Exception thrown when authentication fails.
 * 
 * <p>This exception should be used when a user cannot be authenticated,
 * typically due to invalid credentials, missing authentication token, or expired token.
 * The handler will return HTTP 401 Unauthorized status.
 */
public class AuthenticationException extends RuntimeException {
    
    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}

