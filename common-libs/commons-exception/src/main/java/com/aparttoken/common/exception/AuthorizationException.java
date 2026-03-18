package com.aparttoken.common.exception;

/**
 * Exception thrown when authorization fails.
 * 
 * <p>This exception should be used when a user is authenticated but does not
 * have permission to perform the requested operation. The handler will return
 * HTTP 403 Forbidden status.
 */
public class AuthorizationException extends RuntimeException {
    
    public AuthorizationException(String message) {
        super(message);
    }

    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
}

