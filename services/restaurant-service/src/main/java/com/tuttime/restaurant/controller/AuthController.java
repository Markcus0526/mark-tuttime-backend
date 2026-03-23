package com.tuttime.restaurant.controller;

import com.tuttime.common.dto.ApiResponse;
import com.tuttime.common.exception.BusinessException;
import com.tuttime.restaurant.dto.*;
import com.tuttime.restaurant.exception.RestaurantServiceMessageKeys;
import com.tuttime.restaurant.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Restaurant authentication endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RestaurantResponse>> register(
            @Valid @RequestBody RestaurantRegistrationRequest request,
            HttpServletRequest httpRequest) {
        log.info("Registration request for email: {}", request.getEmail());

        // Validate email is provided (database requires it)
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new BusinessException(
                    "Email is required",
                    RestaurantServiceMessageKeys.EMAIL_REQUIRED);
        }

        // Create user registration request
        RestaurantRegistrationRequest registrationRequest = RestaurantRegistrationRequest.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .name(request.getName())
                .country(request.getCountry())
                .city(request.getCity())
                .build();

        // Complete signup in a single transaction (will rollback on any error)
        RestaurantResponse loginResponse = authService.registerRestaurant(registrationRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Restaurant registered successfully", loginResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<RestaurantLoginResponse>> login(
            @Valid @RequestBody RestaurantLoginRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = extractIpAddress(httpRequest);

        RestaurantLoginResponse response = authService.login(
                request.getEmail(),
                request.getPassword(),
                ipAddress
        );

        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    private String extractIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}