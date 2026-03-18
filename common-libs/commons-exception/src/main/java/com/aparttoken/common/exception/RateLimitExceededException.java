package com.aparttoken.common.exception;

/**
 * Exception thrown when rate limit is exceeded.
 * 
 * <p>This exception should be used when a client has exceeded the allowed
 * number of requests within a time period. The handler will return HTTP 429
 * Too Many Requests status with a Retry-After header indicating when the
 * client can retry.
 */
public class RateLimitExceededException extends RuntimeException {
    
    private final int retryAfterSeconds;
    
    public RateLimitExceededException(String message, int retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public RateLimitExceededException(String message, int retryAfterSeconds, Throwable cause) {
        super(message, cause);
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    /**
     * Returns the number of seconds the client should wait before retrying the request.
     * 
     * @return retry after seconds
     */
    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}

