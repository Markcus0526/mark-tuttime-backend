package com.tuttime.admin.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Admin Proxy Service: Proxies requests to admin-service using RestTemplate. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProxyServiceImpl implements AdminProxyService {

    @Value("${admin.service.url:http://admin-service}")
    private String adminServiceUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final StandardServletMultipartResolver multipartResolver = new StandardServletMultipartResolver();

    @Override
    public ResponseEntity<Object> proxyRequest(
            jakarta.servlet.http.HttpServletRequest request,
            String authorization) {
        return proxyRequest(request, authorization, null);
    }

    @Override
    public ResponseEntity<Object> proxyRequest(
            jakarta.servlet.http.HttpServletRequest request,
            String authorization,
            Object body) {
        
        try {
            // Build target URL
            String path = request.getRequestURI();
            String queryString = request.getQueryString();
            URI targetUri = UriComponentsBuilder
                    .fromHttpUrl(adminServiceUrl + path)
                    .query(queryString)
                    .build()
                    .toUri();

            HttpHeaders headers = new HttpHeaders();
            
            // Preserve original Content-Type (important for multipart/form-data boundary)
            String contentType = request.getContentType();
            MediaType mediaType = null;
            if (contentType != null && !contentType.isEmpty()) {
                mediaType = MediaType.parseMediaType(contentType);
                headers.setContentType(mediaType);
                log.debug("Preserved Content-Type: {}", contentType);
            }
            
            // Copy other headers (except Host, Content-Length, Content-Type, Authorization)
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String lowerHeaderName = headerName.toLowerCase();
                if (!lowerHeaderName.equals("host") && 
                    !lowerHeaderName.equals("content-length") &&
                    !lowerHeaderName.equals("content-type") &&
                    !lowerHeaderName.equals("authorization")) {
                    headers.set(headerName, request.getHeader(headerName));
                }
            }
            
            // Set Authorization header (from parameter or from request)
            String authHeader = authorization != null ? authorization : request.getHeader("Authorization");
            if (authHeader != null && !authHeader.isEmpty()) {
                // Ensure Authorization header is properly formatted
                if (!authHeader.startsWith("Bearer ") && !authHeader.startsWith("bearer ")) {
                    authHeader = "Bearer " + authHeader;
                }
                headers.set("Authorization", authHeader);
                
                // Log token info for debugging (without exposing full token)
                String tokenPart = authHeader.replace("Bearer ", "").replace("bearer ", "");
                if (tokenPart.length() > 20) {
                    log.debug("Forwarding Authorization header to admin-service (token length: {}, starts with: {}...)", 
                            tokenPart.length(), tokenPart.substring(0, 20));
                } else {
                    log.warn("Authorization token appears to be too short (length: {}). Expected JWT format.", 
                            tokenPart.length());
                }
            } else {
                // No Authorization header - expected for login/logout endpoints
                String requestPath = request.getRequestURI();
                boolean isAuthEndpoint = requestPath != null && (
                    requestPath.endsWith("/login") || 
                    requestPath.contains("/auth/login") ||
                    requestPath.endsWith("/logout") ||
                    requestPath.contains("/auth/logout")
                );
                
                if (isAuthEndpoint) {
                    log.debug("No Authorization header found for auth endpoint: {} (expected for login)", requestPath);
                } else {
                    log.warn("No Authorization header found for protected endpoint: {}", requestPath);
                }
            }

            String methodStr = request.getMethod();
            if (methodStr == null) {
                throw new IllegalArgumentException("HTTP method cannot be null");
            }
            HttpMethod method = HttpMethod.valueOf(methodStr);

            // Handle multipart/form-data specially
            HttpEntity<?> requestEntity;
            if (mediaType != null && mediaType.isCompatibleWith(MediaType.MULTIPART_FORM_DATA)) {
                // Parse multipart request if not already parsed
                MultipartHttpServletRequest multipartRequest = null;
                if (request instanceof MultipartHttpServletRequest) {
                    multipartRequest = (MultipartHttpServletRequest) request;
                } else if (multipartResolver.isMultipart(request)) {
                    try {
                        multipartRequest = multipartResolver.resolveMultipart(request);
                        log.debug("Manually parsed multipart request");
                    } catch (Exception e) {
                        log.warn("Failed to parse multipart request: {}", e.getMessage());
                    }
                }
                
                if (multipartRequest != null) {
                    org.springframework.util.MultiValueMap<String, Object> parts = 
                            new org.springframework.util.LinkedMultiValueMap<>();
                    
                    // Add form parameters
                    Map<String, String[]> parameterMap = multipartRequest.getParameterMap();
                    for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                        String key = entry.getKey();
                        String[] values = entry.getValue();
                        if (values != null && values.length > 0) {
                            parts.add(key, values[0]); // Take first value
                        }
                    }
                    
                    // Add multipart files
                    Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
                    for (Map.Entry<String, MultipartFile> entry : fileMap.entrySet()) {
                        parts.add(entry.getKey(), entry.getValue().getResource());
                        log.debug("Added multipart file: key={}, fileName={}, size={}", 
                                entry.getKey(), entry.getValue().getOriginalFilename(), 
                                entry.getValue().getSize());
                    }
                    
                    // Check file parts
                    Iterator<String> fileNames = multipartRequest.getFileNames();
                    while (fileNames.hasNext()) {
                        String fileName = fileNames.next();
                        MultipartFile file = multipartRequest.getFile(fileName);
                        if (file != null && !file.isEmpty()) {
                            parts.add(fileName, file.getResource());
                            log.debug("Added multipart file from iterator: key={}, fileName={}, size={}", 
                                    fileName, file.getOriginalFilename(), file.getSize());
                        }
                    }
                    
                    log.info("Proxying multipart request with {} parts: keys={}", 
                            parts.size(), parts.keySet());
                    
                    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
                    requestEntity = new HttpEntity<>(parts, headers);
                } else {
                    // Request not parsed as multipart - read as raw bytes
                    log.warn("Multipart request not parsed - reading as raw bytes.");
                    try {
                        byte[] bodyBytes = StreamUtils.copyToByteArray(request.getInputStream());
                        if (bodyBytes.length > 0) {
                            log.debug("Proxying multipart as raw bytes: size={} bytes, contentType={}", 
                                    bodyBytes.length, contentType);
                            headers.setContentLength(bodyBytes.length);
                            requestEntity = new HttpEntity<>(bodyBytes, headers);
                        } else {
                            requestEntity = new HttpEntity<>(headers);
                        }
                    } catch (IOException e) {
                        log.error("Failed to read multipart request body", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("error", "Failed to read request body: " + e.getMessage()));
                    }
                }
            } else {
                // For non-multipart requests (JSON, etc.)
                if (body != null) {
                    // Use provided body (already read by @RequestBody)
                    // Ensure Content-Type is set for JSON if not already set
                    if (contentType == null || contentType.isEmpty()) {
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        log.debug("Set Content-Type to application/json for body request");
                    }
                    log.debug("Proxying request with provided body: contentType={}, bodyType={}", 
                            headers.getContentType(), body.getClass().getSimpleName());
                    requestEntity = new HttpEntity<>(body, headers);
                } else {
                    // Read from request stream (if not already consumed)
                    try {
                        byte[] bodyBytes = StreamUtils.copyToByteArray(request.getInputStream());
                        if (bodyBytes.length > 0) {
                            log.debug("Proxying request: size={} bytes, contentType={}", 
                                    bodyBytes.length, contentType);
                            
                            // For JSON content, deserialize and let RestTemplate serialize it properly
                            if (contentType != null && contentType.contains("application/json")) {
                                try {
                                    String jsonBody = new String(bodyBytes, StandardCharsets.UTF_8);
                                    log.debug("Proxying JSON request: {}", jsonBody);
                                    // Deserialize to Object so RestTemplate can serialize it properly
                                    Object jsonObject = objectMapper.readValue(jsonBody, Object.class);
                                    headers.setContentType(MediaType.APPLICATION_JSON);
                                    requestEntity = new HttpEntity<>(jsonObject, headers);
                                } catch (Exception e) {
                                    log.warn("Failed to parse JSON body, sending as raw bytes: {}", e.getMessage());
                                    headers.setContentType(MediaType.APPLICATION_JSON);
                                    headers.setContentLength(bodyBytes.length);
                                    requestEntity = new HttpEntity<>(bodyBytes, headers);
                                }
                            } else {
                                headers.setContentLength(bodyBytes.length);
                                requestEntity = new HttpEntity<>(bodyBytes, headers);
                            }
                        } else {
                            requestEntity = new HttpEntity<>(headers);
                        }
                    } catch (IOException e) {
                        log.error("Failed to read request body", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("error", "Failed to read request body: " + e.getMessage()));
                    }
                }
            }

            long proxyStartTime = System.currentTimeMillis();
            log.debug("Proxying {} request to admin-service: {}", method, targetUri);
            
            ResponseEntity<String> stringResponse;
            long restTemplateCallDuration;
            try {
                stringResponse = restTemplate.exchange(
                        targetUri,
                        method,
                        requestEntity,
                        String.class
                );
                restTemplateCallDuration = System.currentTimeMillis() - proxyStartTime;
                
                // Alert: Request latency > 1 second
                if (restTemplateCallDuration > 1000) {
                    log.warn("ALERT: Slow request detected - path={}, duration={}ms, status={}", 
                            request.getRequestURI(), restTemplateCallDuration, stringResponse.getStatusCode());
                }
            } catch (org.springframework.web.client.ResourceAccessException e) {
                // Alert: Connection pool exhaustion or connection errors (including timeouts)
                long errorDuration = System.currentTimeMillis() - proxyStartTime;
                if (e.getCause() instanceof java.net.SocketTimeoutException) {
                    log.error("ALERT: Connection timeout - path={}, duration={}ms, timeout={}", 
                            request.getRequestURI(), errorDuration, e.getMessage());
                } else {
                    log.error("ALERT: Connection error - path={}, duration={}ms, error={}", 
                            request.getRequestURI(), errorDuration, e.getMessage());
                }
                throw e;
            }
            
            MediaType responseContentType = stringResponse.getHeaders().getContentType();
            log.debug("Response contentType: {}", responseContentType);

            // Parse JSON string to Object to avoid double-serialization            
            Object responseBody = stringResponse.getBody();
            boolean isJsonResponse = responseContentType != null && (
                    responseContentType.includes(MediaType.APPLICATION_JSON) || 
                    responseContentType.getType().equals("application") && responseContentType.getSubtype().contains("json")
            );
            
            if (responseBody != null && isJsonResponse) {
                try {
                    String jsonString = (String) responseBody;
                    log.debug("Parsing JSON response body (length: {} bytes)", jsonString.length());
                    responseBody = objectMapper.readValue(jsonString, Object.class);
                } catch (Exception e) {
                    log.error("Failed to parse JSON response, returning as String: {}", e.getMessage());
                }
            }
            
            // Create response headers, filtering out CORS headers from admin-service
            HttpHeaders responseHeaders = new HttpHeaders();
            for (Map.Entry<String, List<String>> entry : stringResponse.getHeaders().entrySet()) {
                String headerName = entry.getKey();
                String lowerHeaderName = headerName.toLowerCase();
                // Filter out CORS-related headers, connection headers, and Content-Length
                // Connection header is invalid in HTTP/2 and causes ERR_HTTP2_PROTOCOL_ERROR
                if (!lowerHeaderName.startsWith("access-control-") && 
                    !lowerHeaderName.equals("connection") &&
                    !lowerHeaderName.equals("keep-alive") &&
                    !lowerHeaderName.equals("content-length")) {
                    responseHeaders.put(headerName, entry.getValue());
                } else {
                    log.debug("Filtered out header from admin-service response: {}", headerName);
                }
            }
            
            // Ensure Content-Type is set for JSON responses
            if (isJsonResponse && responseContentType != null) {
                responseHeaders.setContentType(responseContentType);
            }
            
            // DO NOT set Connection: close header (invalid in HTTP/2, causes ERR_HTTP2_PROTOCOL_ERROR)
            
            long totalProxyDuration = System.currentTimeMillis() - proxyStartTime;
            
            // Alert: Total request latency > 1 second
            if (totalProxyDuration > 1000) {
                log.warn("ALERT: Slow proxy request - path={}, totalDuration={}ms, status={}", 
                        request.getRequestURI(), totalProxyDuration, stringResponse.getStatusCode());
            }
            
            ResponseEntity<Object> response = ResponseEntity.status(stringResponse.getStatusCode())
                    .headers(responseHeaders)
                    .body(responseBody);
            
            return response;

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Forward client errors (4xx) from admin-service
            log.warn("Admin-service returned error: status={}, path={}, message={}", 
                    e.getStatusCode(), request.getRequestURI(), e.getMessage());
            
            try {
                String responseBody = e.getResponseBodyAsString();
                if (responseBody != null && !responseBody.isEmpty() && !responseBody.trim().equals("[no body]")) {
                    return ResponseEntity.status(e.getStatusCode())
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(responseBody);
                }
                
                // Provide error message based on status code
                String errorMessage;
                if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                    errorMessage = "Access forbidden. Please ensure you are authenticated with a valid JWT token.";
                } else if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    errorMessage = "Unauthorized. Please provide a valid Authorization header with Bearer token.";
                } else {
                    errorMessage = "Request failed with status " + e.getStatusCode();
                }
                
                return ResponseEntity.status(e.getStatusCode())
                        .body(Map.of(
                                "error", errorMessage,
                                "status", e.getStatusCode().value(),
                                "path", request.getRequestURI()
                        ));
            } catch (Exception parseException) {
                log.error("Error parsing error response: {}", parseException.getMessage());
                return ResponseEntity.status(e.getStatusCode())
                        .body(Map.of("error", "Request failed: " + e.getStatusCode()));
            }
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // Alert: Connection pool exhaustion or connection errors
            log.error("ALERT: Connection error - path={}, url={}, error={}", 
                    request.getRequestURI(), adminServiceUrl, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of(
                            "error", "Cannot connect to admin-service",
                            "message", "Admin service is not available. Please ensure admin-service is running on " + adminServiceUrl,
                            "details", e.getMessage() != null ? e.getMessage() : "Connection failed"
                    ));
        } catch (Exception e) {
            log.error("Error proxying request to admin-service: url={}, path={}, error={}", 
                    adminServiceUrl, request.getRequestURI(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to proxy request to admin-service",
                            "message", e.getMessage() != null ? e.getMessage() : "Unknown error occurred",
                            "url", adminServiceUrl + request.getRequestURI()
                    ));
        }
    }
}

