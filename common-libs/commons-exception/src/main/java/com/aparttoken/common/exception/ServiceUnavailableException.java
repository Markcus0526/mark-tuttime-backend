package com.aparttoken.common.exception;

/**
 * Exception thrown when a required service is unavailable.
 * 
 * <p>This exception should be used when a downstream service or dependency
 * is temporarily unavailable. The handler will return HTTP 503 Service
 * Unavailable status.
 */
public class ServiceUnavailableException extends RuntimeException {
    
    private final String serviceName;
    
    public ServiceUnavailableException(String serviceName, String message) {
        super(message);
        this.serviceName = serviceName;
    }

    public ServiceUnavailableException(String serviceName, String message, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
    }
    
    /**
     * Returns the name of the unavailable service.
     * 
     * @return service name
     */
    public String getServiceName() {
        return serviceName;
    }
}

