package com.tuttime.user.controller;

import com.tuttime.common.dto.ApiResponse;
import com.tuttime.user.dto.AppVersionResponse;
import com.tuttime.user.service.AppVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for App Version Operations
 *
 * <p>This controller handles HTTP requests related to app version information,
 * allowing authenticated users to retrieve current app version numbers and install links.
 *
 * <p><b>Authenticated Endpoints:</b>
 * <ul>
 *   <li>GET /api/v1/app/version - Get current app version and install links</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/app/version")
@RequiredArgsConstructor
@Tag(name = "App Version", description = "App version information APIs for authenticated users")
public class AppVersionController {

    private final AppVersionService appVersionService;

    /**
     * Get current app version and install links
     *
     * <p>Returns the current active app version configuration including version numbers
     * and install links for both Android and iOS platforms.
     * 
     * <p><b>Authentication:</b> Required (user JWT token)
     * 
     * @param userId Authenticated user ID (from JWT token)
     * @return ResponseEntity containing app version information
     */
    @GetMapping
    @Operation(
        summary = "Get app version", 
        description = "Returns current app version numbers and install links for Android and iOS. Requires user authentication."
    )
    public ResponseEntity<ApiResponse<AppVersionResponse>> getAppVersion(
            @AuthenticationPrincipal Long userId) {
        
        log.debug("Getting app version - requested by user: {}", userId);
        AppVersionResponse version = appVersionService.getAppVersion();
        return ResponseEntity.ok(ApiResponse.success("App version retrieved successfully", version));
    }
}

