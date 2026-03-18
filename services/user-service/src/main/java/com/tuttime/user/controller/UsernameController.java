package com.tuttime.user.controller;

import com.tuttime.common.dto.ApiResponse;
import com.tuttime.user.service.query.UserQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public endpoint for looking up a user ID by username.
 * Path: /api/v1/username/{username}/id
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/username")
@RequiredArgsConstructor
@Tag(name = "Username Lookup", description = "Look up user IDs by username")
public class UsernameController {

    private final UserQueryService userQueryService;

    @GetMapping("/{username}/id")
    @Operation(summary = "Get user ID by username",
               description = "Returns the user ID for a given username (active, non-deleted users only)")
    public ResponseEntity<ApiResponse<Long>> getUserIdByUsername(
            @PathVariable String username) {
        log.info("Username lookup – getting user ID for username: {}", username);
        Long userId = userQueryService.getUserIdByUsername(username);
        return ResponseEntity.ok(ApiResponse.success(userId));
    }
}

