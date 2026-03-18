package com.aparttoken.common.exception;

/**
 * Standardized error codes for consistent error handling across all services.
 * 
 * <p>These error codes provide a standardized way to identify error types,
 * allowing clients to handle errors consistently regardless of which service
 * generated the error.
 */
public final class ErrorCodes {
    
    // Client Error Codes (4xx)
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
    public static final String NOT_ACCEPTABLE = "NOT_ACCEPTABLE";
    public static final String CONFLICT = "CONFLICT";
    public static final String PRECONDITION_FAILED = "PRECONDITION_FAILED";
    public static final String PAYLOAD_TOO_LARGE = "PAYLOAD_TOO_LARGE";
    public static final String URI_TOO_LONG = "URI_TOO_LONG";
    public static final String UNSUPPORTED_MEDIA_TYPE = "UNSUPPORTED_MEDIA_TYPE";
    public static final String UNPROCESSABLE_ENTITY = "UNPROCESSABLE_ENTITY";
    public static final String LOCKED = "LOCKED";
    public static final String TOO_MANY_REQUESTS = "TOO_MANY_REQUESTS";
    
    // Validation Error Codes
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String INVALID_INPUT = "INVALID_INPUT";
    public static final String MISSING_REQUIRED_FIELD = "MISSING_REQUIRED_FIELD";
    public static final String INVALID_FORMAT = "INVALID_FORMAT";
    public static final String OUT_OF_RANGE = "OUT_OF_RANGE";
    
    // Business Logic Error Codes
    public static final String BUSINESS_RULE_VIOLATION = "BUSINESS_RULE_VIOLATION";
    public static final String INSUFFICIENT_BALANCE = "INSUFFICIENT_BALANCE";
    public static final String RESOURCE_ALREADY_EXISTS = "RESOURCE_ALREADY_EXISTS";
    public static final String OPERATION_NOT_ALLOWED = "OPERATION_NOT_ALLOWED";
    public static final String INVALID_STATE = "INVALID_STATE";
    public static final String QUOTA_EXCEEDED = "QUOTA_EXCEEDED";
    
    // Server Error Codes (5xx)
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    public static final String NOT_IMPLEMENTED = "NOT_IMPLEMENTED";
    public static final String BAD_GATEWAY = "BAD_GATEWAY";
    public static final String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
    public static final String GATEWAY_TIMEOUT = "GATEWAY_TIMEOUT";
    
    // Network Error Codes
    public static final String CONNECTION_TIMEOUT = "CONNECTION_TIMEOUT";
    public static final String SERVICE_UNABLE_TO_CONNECT = "SERVICE_UNABLE_TO_CONNECT";
    
    // Unknown Error
    public static final String UNKNOWN_ERROR = "UNKNOWN_ERROR";
    
    private ErrorCodes() {
        // Utility class - prevent instantiation
    }
}
