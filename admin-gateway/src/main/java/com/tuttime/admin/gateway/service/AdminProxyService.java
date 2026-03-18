package com.tuttime.admin.gateway.service;

import org.springframework.http.ResponseEntity;

/** Admin Proxy Service Interface: proxies requests to admin-service. */
public interface AdminProxyService {
    ResponseEntity<Object> proxyRequest(jakarta.servlet.http.HttpServletRequest request, String authorization);
    ResponseEntity<Object> proxyRequest(jakarta.servlet.http.HttpServletRequest request, String authorization, Object body);
}

