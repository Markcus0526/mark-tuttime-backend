package com.tuttime.gateway.exception;

import com.aparttoken.common.exception.ErrorCodes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/** Global Exception Handler: handles exceptions globally and returns consistent error responses with correlation ID and error codes. */
@Slf4j
@Component
@Order(-2) // Higher priority than default error handler
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    
    private final ObjectMapper objectMapper;

    public GlobalExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String errorCode = ErrorCodes.INTERNAL_SERVER_ERROR;
        String message = "An unexpected error occurred";

        if (ex instanceof ResponseStatusException) {
            ResponseStatusException responseStatusException = (ResponseStatusException) ex;
            status = HttpStatus.resolve(responseStatusException.getStatusCode().value());
            if (status == null) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }
            message = responseStatusException.getReason() != null 
                    ? responseStatusException.getReason() 
                    : status.getReasonPhrase();
        } else if (ex instanceof org.springframework.web.server.ServerWebInputException) {
            status = HttpStatus.BAD_REQUEST;
            errorCode = ErrorCodes.BAD_REQUEST;
            message = "Invalid request parameters";
        } else if (ex instanceof java.util.concurrent.TimeoutException) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            errorCode = ErrorCodes.GATEWAY_TIMEOUT;
            message = "Request timeout";
        } else if (ex instanceof org.springframework.cloud.gateway.support.NotFoundException) {
            status = HttpStatus.NOT_FOUND;
            errorCode = ErrorCodes.RESOURCE_NOT_FOUND;
            message = "Resource not found";
        }

        errorCode = mapStatusToErrorCode(status);
        String correlationId = getCorrelationId(exchange);
        String path = exchange.getRequest().getURI().getPath();
        
        // Log error with shortened stack trace (first 5 lines for unexpected errors)
        String stackTraceSummary = com.aparttoken.common.exception.StackTraceUtil.getStackTraceSummary(ex, 5);
        log.error("Global exception handler: correlationId={}, errorCode={}, message={}, path={}, stackTrace={}", 
                correlationId, errorCode, message, path, stackTraceSummary);

        // Build standardized error response matching ErrorResponse structure
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", OffsetDateTime.now(ZoneOffset.UTC).format(ISO_FORMATTER));
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("errorCode", errorCode);
        errorResponse.put("message", message);
        errorResponse.put("path", path);
        errorResponse.put("correlationId", correlationId);

        // Set response status and headers
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        // Ensure correlation ID is in response headers
        if (correlationId != null) {
            exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);
        }

        // Write error response
        try {
            String json = objectMapper.writeValueAsString(errorResponse);
            DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
            DataBuffer buffer = bufferFactory.wrap(json.getBytes(StandardCharsets.UTF_8));
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            // Critical error - include shortened stack trace
            String errorStackTrace = com.aparttoken.common.exception.StackTraceUtil.getStackTraceSummary(e, 3);
            log.error("Failed to serialize error response, stackTrace={}", errorStackTrace);
            return Mono.empty();
        }
    }

    /** Extracts correlation ID from request headers or exchange attributes. */
    private String getCorrelationId(ServerWebExchange exchange) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = (String) exchange.getAttributes().get(CORRELATION_ID_HEADER);
        }
        
        return correlationId;
    }

    /** Maps HTTP status code to standardized error code using ErrorCodes constants. */
    private String mapStatusToErrorCode(HttpStatus status) {
        return switch (status.value()) {
            case 400 -> ErrorCodes.BAD_REQUEST;
            case 401 -> ErrorCodes.UNAUTHORIZED;
            case 403 -> ErrorCodes.FORBIDDEN;
            case 404 -> ErrorCodes.RESOURCE_NOT_FOUND;
            case 405 -> ErrorCodes.METHOD_NOT_ALLOWED;
            case 406 -> ErrorCodes.NOT_ACCEPTABLE;
            case 409 -> ErrorCodes.CONFLICT;
            case 412 -> ErrorCodes.PRECONDITION_FAILED;
            case 413 -> ErrorCodes.PAYLOAD_TOO_LARGE;
            case 414 -> ErrorCodes.URI_TOO_LONG;
            case 415 -> ErrorCodes.UNSUPPORTED_MEDIA_TYPE;
            case 422 -> ErrorCodes.UNPROCESSABLE_ENTITY;
            case 423 -> ErrorCodes.LOCKED;
            case 429 -> ErrorCodes.TOO_MANY_REQUESTS;
            case 500 -> ErrorCodes.INTERNAL_SERVER_ERROR;
            case 501 -> ErrorCodes.NOT_IMPLEMENTED;
            case 502 -> ErrorCodes.BAD_GATEWAY;
            case 503 -> ErrorCodes.SERVICE_UNAVAILABLE;
            case 504 -> ErrorCodes.GATEWAY_TIMEOUT;
            default -> ErrorCodes.UNKNOWN_ERROR;
        };
    }
}

