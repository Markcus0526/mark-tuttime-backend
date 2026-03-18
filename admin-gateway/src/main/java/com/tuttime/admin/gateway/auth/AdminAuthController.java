package com.tuttime.admin.gateway.auth;

import com.tuttime.admin.gateway.service.AdminProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Admin Authentication Controller: proxies admin login/logout requests to admin-service (stateless, no authentication logic). */
@Slf4j
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminProxyService adminProxyService;

    @PostMapping("/login")
    public ResponseEntity<Object> login(
            jakarta.servlet.http.HttpServletRequest request) {
        
        log.debug("Proxying login request to admin-service");
        return adminProxyService.proxyRequest(request, null);
    }

    @PostMapping("/logout")
    public ResponseEntity<Object> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            jakarta.servlet.http.HttpServletRequest request) {
        
        log.debug("Proxying logout request to admin-service, Authorization header present: {}", 
                authorization != null && !authorization.isEmpty());
        return adminProxyService.proxyRequest(request, authorization);
    }
}

