package com.tuttime.admin.gateway.controller;

import com.tuttime.admin.gateway.service.AdminProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Admin Proxy Controller: thin proxy that forwards all admin requests to admin-service (no business logic). */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminProxyController {

    private final AdminProxyService adminProxyService;

    /** Proxy all GET requests to admin-service */
    @GetMapping("/**")
    public ResponseEntity<Object> proxyGet(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            jakarta.servlet.http.HttpServletRequest request) {
        
        log.debug("Proxying GET request to admin-service: path={}", request.getRequestURI());
        return adminProxyService.proxyRequest(request, authorization);
    }

    /** Proxy all POST requests to admin-service (supports JSON and multipart/form-data). */
    @PostMapping("/**")
    public ResponseEntity<Object> proxyPost(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            jakarta.servlet.http.HttpServletRequest request) {
        
        log.debug("Proxying POST request to admin-service: path={}, contentType={}", 
                request.getRequestURI(), request.getContentType());
        return adminProxyService.proxyRequest(request, authorization);
    }

    /** Proxy all PUT requests to admin-service (supports JSON and multipart/form-data). */
    @PutMapping("/**")
    public ResponseEntity<Object> proxyPut(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            jakarta.servlet.http.HttpServletRequest request) {
        
        log.debug("Proxying PUT request to admin-service: path={}, contentType={}", 
                request.getRequestURI(), request.getContentType());
        return adminProxyService.proxyRequest(request, authorization);
    }

    /** Proxy all DELETE requests to admin-service */
    @DeleteMapping("/**")
    public ResponseEntity<Object> proxyDelete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            jakarta.servlet.http.HttpServletRequest request) {
        
        log.debug("Proxying DELETE request to admin-service: path={}", request.getRequestURI());
        return adminProxyService.proxyRequest(request, authorization);
    }
}

