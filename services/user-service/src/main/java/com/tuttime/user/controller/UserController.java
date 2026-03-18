package com.tuttime.user.controller;

import com.tuttime.common.dto.ApiResponse;
import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.user.dto.*;
import com.tuttime.user.repository.UserRepository;
import com.tuttime.user.service.AuthService;
import com.tuttime.user.service.EmailVerificationService;
import com.tuttime.user.service.ImageStorageService;
import com.tuttime.user.service.impl.S3ImageStorageServiceImpl;
import com.tuttime.user.dto.ProfileImageResult;
import com.tuttime.user.util.ImageUrlHelper;
import com.tuttime.user.service.OnlineStatusService;
import com.tuttime.user.service.UserService;
import com.tuttime.user.service.UserTokenStatusService;
import com.tuttime.user.service.query.UserQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

/** REST Controller for User Management: handles user registration, authentication, and user information retrieval. Uses CQRS pattern. */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User registration and management APIs")
public class UserController {
    
    private final ObjectMapper objectMapper;

    private final UserService userService;
    private final UserQueryService userQueryService;
    private final AuthService authService;
    private final ImageStorageService imageStorageService;
    private final UserTokenStatusService userTokenStatusService;
    private final EmailVerificationService emailVerificationService;
    private final UserRepository userRepository;
    private final OnlineStatusService onlineStatusService;
    private final com.tuttime.user.service.ApartSharingStatusService apartSharingStatusService;
    private final com.tuttime.user.service.UserBlockService userBlockService;
    private final com.tuttime.user.service.UserReportService userReportService;

    /** Registers new user: validates request (email, password, phone), creates account. Returns 201 on success, 400 on validation failure, 409 if user exists. */
    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account with email and password")
    public ResponseEntity<ApiResponse<UserResponse>> registerUser(
            @Valid @RequestBody UserRegistrationRequest request) {
        log.info("Received registration request for email: {}", request.getEmail());
        UserResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    /** Authenticates user and returns JWT token */
    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticates user and returns JWT token")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Logs out a user by invalidating their JWT token.
     *
     * <p>This endpoint blacklists the provided JWT token in Redis, preventing
     * it from being used for subsequent authenticated requests. The token will
     * remain blacklisted until it naturally expires.
     *
     * <p><b>Logout Flow:</b>
     * <ol>
     *   <li>Extract token from Authorization header</li>
     *   <li>Validate token and extract user ID</li>
     *   <li>Add token to Redis blacklist with TTL matching token expiration</li>
     *   <li>Return success response</li>
     * </ol>
     *
     * <p><b>Response:</b>
     * <ul>
     *   <li>HTTP 200 OK on successful logout</li>
     *   <li>HTTP 400 Bad Request if token is invalid</li>
     *   <li>HTTP 401 Unauthorized if token is missing or expired</li>
     * </ul>
     *
     * @param authorizationHeader The Authorization header containing the Bearer token
     * @return ResponseEntity containing success message
     */
    /**
     * Refreshes the access token using a refresh token.
     *
     * <p>This endpoint is critical for mobile apps to maintain user sessions without
     * requiring re-authentication. When the access token expires, mobile apps can
     * use this endpoint to obtain a new access token and refresh token pair.
     *
     * <p><b>Refresh Token Flow:</b>
     * <ol>
     *   <li>Validate refresh token signature and expiration</li>
     *   <li>Check if refresh token is blacklisted (revoked)</li>
     *   <li>Verify token type is "refresh"</li>
     *   <li>Check user account status (active, not blocked)</li>
     *   <li>Generate new access token and refresh token</li>
     *   <li>Blacklist old refresh token (optional, for enhanced security)</li>
     *   <li>Return new token pair</li>
     * </ol>
     *
     * <p><b>Response:</b>
     * <ul>
     *   <li>HTTP 200 OK with new token pair on success</li>
     *   <li>HTTP 401 Unauthorized if refresh token is invalid or expired</li>
     *   <li>HTTP 400 Bad Request if request validation fails</li>
     * </ul>
     *
     * @param request The refresh token request containing the refresh token
     * @return ResponseEntity containing new access token and refresh token
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Generates new access token and refresh token using a valid refresh token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        log.info("Refresh token request received");
        LoginResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    /**
     * Logs out a user by invalidating their JWT token.
     *
     * <p>This endpoint blacklists the provided JWT token in Redis, preventing
     * it from being used for subsequent authenticated requests. The token will
     * remain blacklisted until it naturally expires.
     *
     * <p><b>Logout Flow:</b>
     * <ol>
     *   <li>Extract token from Authorization header</li>
     *   <li>Validate token and extract user ID</li>
     *   <li>Add token to Redis blacklist with TTL matching token expiration</li>
     *   <li>Return success response</li>
     * </ol>
     *
     * <p><b>Response:</b>
     * <ul>
     *   <li>HTTP 200 OK on successful logout</li>
     *   <li>HTTP 400 Bad Request if token is invalid</li>
     *   <li>HTTP 401 Unauthorized if token is missing or expired</li>
     * </ul>
     *
     * @param authorizationHeader The Authorization header containing the Bearer token
     * @return ResponseEntity containing success message
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Invalidates JWT token by adding it to blacklist")
    public ResponseEntity<ApiResponse<String>> logout(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new com.tuttime.common.exception.AuthenticationException("Authorization token is required");
        }

        String token = authorizationHeader.substring(7); // Remove "Bearer " prefix
        
        // Extract user ID from token
        Long userId = authService.getUserIdFromToken(token);
        if (userId == null) {
            throw new com.tuttime.common.exception.AuthenticationException("Invalid token");
        }

        // Blacklist the token
        authService.logout(token, userId);
        
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }

    /**
     * Retrieves user ID by username.
     *
     * <p>This endpoint returns the user ID for a given username. Useful for
     * looking up users by their username.
     *
     * <p><b>Response:</b>
     * <ul>
     *   <li>HTTP 200 OK with user ID on success</li>
     *   <li>HTTP 404 Not Found if user doesn't exist</li>
     * </ul>
     *
     * @param username The username to look up
     * @return ResponseEntity containing user ID
     */
    @GetMapping("/username/{username}/id")
    @Operation(summary = "Get user ID by username", description = "Retrieves user ID for a given username")
    public ResponseEntity<ApiResponse<Long>> getUserIdByUsername(
            @PathVariable String username) {
        log.info("Getting user ID for username: {}", username);
        Long userId = userQueryService.getUserIdByUsername(username);
        return ResponseEntity.ok(ApiResponse.success(userId));
    }

    /**
     * Retrieves user information by user ID.
     *
     * <p>This endpoint uses the CQRS pattern with a dedicated query service
     * that is optimized for read operations. The query service may use caching
     * to improve performance for frequently accessed users.
     *
     * <p><b>CQRS Benefits:</b>
     * <ul>
     *   <li>Optimized read queries (separate from write operations)</li>
     *   <li>Caching support for frequently accessed data</li>
     *   <li>Read models can be denormalized for better performance</li>
     * </ul>
     *
     * <p><b>Response:</b>
     * <ul>
     *   <li>HTTP 200 OK with user information on success</li>
     *   <li>HTTP 404 Not Found if user doesn't exist</li>
     * </ul>
     *
     * @param userId The unique identifier of the user to retrieve
     * @return ResponseEntity containing user information
     */
    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID", description = "Retrieves user information by user ID (uses CQRS query service). Returns minimal info (userId, username) if requesting user has blocked the target user.")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @AuthenticationPrincipal Long requestingUserId,
            @PathVariable Long userId) {
        // Check if requesting user has blocked the target user (one-way check)
        if (requestingUserId != null && userBlockService.hasBlocked(requestingUserId, userId)) {
            // Return minimal response (only userId and username) when user has blocked the target
            log.debug("User {} has blocked user {}, returning minimal response", requestingUserId, userId);
            com.tuttime.user.entity.User user = userRepository.findById(userId).orElse(null);
            if (user == null || user.getDeletedAt() != null) {
                throw new ResourceNotFoundException("User not found");
            }
            UserResponse minimalResponse = UserResponse.builder()
                    .userId(userId)
                    .username(user.getUsername())
                    .build();
            return ResponseEntity.ok(ApiResponse.success(minimalResponse));
        }
        // CQRS: Use query service for reads (optimized with caching and read models)
        UserResponse response = userQueryService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Gets the current authenticated user's profile.
     *
     * @param userId User ID from authentication principal
     * @return UserResponse containing current user information
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Retrieves the current authenticated user's profile")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal Long userId) {
        log.info("Getting current user profile for userId: {}", userId);
        UserResponse response = userService.getCurrentUser(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get FCM tokens for a user's active devices
     * 
     * <p>This endpoint is used by notification-service to fetch FCM tokens
     * for sending push notifications to a user's active devices.
     * 
     * @param userId User ID
     * @return List of FCM tokens for active devices
     */
    @GetMapping("/{userId}/devices/fcm-tokens")
    @Operation(summary = "Get FCM tokens", 
               description = "Get FCM tokens for a user's active devices. Used by notification-service for push notifications.")
    public ResponseEntity<ApiResponse<List<String>>> getUserFcmTokens(
            @PathVariable Long userId) {
        log.info("Getting FCM tokens for user: userId={}", userId);
        List<String> fcmTokens = userService.getUserFcmTokens(userId);
        return ResponseEntity.ok(ApiResponse.success(fcmTokens));
    }

    /**
     * Gets Apart-sharing registration status for a user.
     * 
     * <p>This endpoint returns cached Apart-sharing registration status from the database.
     * If cache is missing or stale, it will fetch fresh status from GraphQL API and update the cache.
     * 
     * @param userId The user ID
     * @return ResponseEntity containing Apart-sharing registration status
     */
    @GetMapping("/{userId}/apart-sharing-status")
    @Operation(summary = "Get Apart-sharing status", 
               description = "Gets cached Apart-sharing registration status. Fetches fresh status if cache is missing or stale.")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getApartSharingStatus(
            @PathVariable Long userId) {
        log.debug("Getting Apart-sharing status for user: userId={}", userId);
        java.util.Map<String, Object> status = apartSharingStatusService.getApartSharingStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * Forces a refresh of Apart-sharing registration status for a user.
     * 
     * <p>This endpoint bypasses the cache and fetches fresh status from GraphQL API.
     * 
     * @param userId The user ID
     * @return ResponseEntity containing refreshed Apart-sharing registration status
     */
    @PostMapping("/{userId}/apart-sharing-status/refresh")
    @Operation(summary = "Refresh Apart-sharing status", 
               description = "Forces a refresh of Apart-sharing registration status by fetching from GraphQL API.")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> refreshApartSharingStatus(
            @PathVariable Long userId) {
        log.info("Refreshing Apart-sharing status for user: userId={}", userId);
        java.util.Map<String, Object> status = apartSharingStatusService.refreshApartSharingStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * Update FCM token for the current user's device
     * 
     * <p>This endpoint allows the frontend to register/update the FCM token
     * for push notifications. The device is identified by X-Device-Id header
     * or device fingerprint.
     * 
     * <p><b>Usage:</b>
     * <ol>
     *   <li>Frontend gets FCM token from Firebase SDK</li>
     *   <li>Frontend calls this endpoint with the FCM token</li>
     *   <li>Backend stores the token in the device record</li>
     *   <li>Backend can now send push notifications to this device</li>
     * </ol>
     * 
     * @param userId User ID from authentication principal
     * @param request Request containing FCM token
     * @param httpRequest HTTP request to extract device ID header
     * @return Success message
     */
    @PutMapping("/me/devices/fcm-token")
    @Operation(summary = "Update FCM token", 
               description = "Register or update FCM token for the current user's device. Required for push notifications.")
    public ResponseEntity<ApiResponse<String>> updateFcmToken(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateFcmTokenRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("Updating FCM token for user: userId={}", userId);
        
        // Extract device ID from header or request body
        String deviceId = httpRequest != null ? httpRequest.getHeader("X-Device-Id") : null;
        if (deviceId == null || deviceId.trim().isEmpty()) {
            deviceId = request.getDeviceId();
        }
        
        userService.updateFcmToken(userId, request.getFcmToken(), deviceId);
        
        return ResponseEntity.ok(ApiResponse.success("FCM token updated successfully"));
    }

    /**
     * Remove invalid FCM tokens for a user
     * 
     * <p>This endpoint is called by notification-service when FCM returns authentication errors (401)
     * or other invalid token errors. It removes the FCM tokens from the user's devices.
     * 
     * <p><b>Note:</b> This is an internal endpoint called by notification-service.
     * 
     * @param userId User ID
     * @param invalidTokens List of invalid FCM tokens to remove
     * @return Success message with count of removed tokens
     */
    @DeleteMapping("/{userId}/devices/fcm-tokens")
    @Operation(summary = "Remove invalid FCM tokens", 
               description = "Remove invalid FCM tokens for a user. Called by notification-service when FCM returns errors.")
    public ResponseEntity<ApiResponse<String>> removeInvalidFcmTokens(
            @PathVariable("userId") Long userId,
            @RequestBody List<String> invalidTokens) {
        
        log.info("Removing invalid FCM tokens for user: userId={}, count={}", userId, 
                invalidTokens != null ? invalidTokens.size() : 0);
        
        int removedCount = userService.removeInvalidFcmTokens(userId, invalidTokens);
        
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Removed %d invalid FCM token(s)", removedCount)));
    }

    /**
     * Updates the current authenticated user's profile (JSON-only).
     * 
     * <p>This endpoint handles JSON-only profile updates without image upload.
     *
     * @param userId User ID from authentication principal
     * @param request Update request containing new profile data
     * @return Updated UserResponse
     */
    @PatchMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update current user profile (JSON)", 
            description = "Updates the current authenticated user's profile information using JSON.")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUserJson(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid UpdateUserProfileRequest request) {
        log.info("Updating profile (JSON) for userId: {}", userId);
        
        UserResponse response = userService.updateUserProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }
    
    /**
     * Updates the current authenticated user's profile with optional image upload.
     * 
     * <p>This endpoint handles multipart/form-data requests with optional image upload.
     * If image is provided, it will be uploaded to S3 and the profile image URL will be updated.
     *
     * @param userId User ID from authentication principal
     * @param dataJson JSON string containing profile data (optional if image is provided)
     * @param image Optional image file to upload
     * @return Updated UserResponse
     */
    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update current user profile with image", 
            description = "Updates the current authenticated user's profile information. Supports optional image upload.")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUserMultipart(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "data", required = false) String dataJson,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        log.info("Updating profile (multipart) for userId: {}", userId);
        
        UpdateUserProfileRequest request = null;
        
        // Parse JSON data from form-data
        if (dataJson != null && !dataJson.trim().isEmpty()) {
            try {
                log.debug("Parsing JSON data from form-data: {}", dataJson);
                request = objectMapper.readValue(dataJson, UpdateUserProfileRequest.class);
                
                // Validate the parsed request manually since @Valid doesn't work with manual parsing
                if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                    throw new BusinessException("Username is required");
                }
                if (request.getUsername().trim().length() < 3 || request.getUsername().trim().length() > 100) {
                    throw new BusinessException("Username must be between 3 and 100 characters");
                }
                if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                    // Basic email validation
                    if (!request.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                        throw new BusinessException("Invalid email format");
                    }
                }
            } catch (BusinessException e) {
                // Re-throw business exceptions as-is
                throw e;
            } catch (Exception e) {
                log.error("Failed to parse JSON data from form-data: {}", dataJson, e);
                throw new BusinessException("Invalid JSON format in data field: " + e.getMessage());
            }
        } else if (dataJson != null && dataJson.trim().isEmpty()) {
            // Handle case where data field is sent as empty string - treat as empty JSON object
            log.debug("Empty data field provided, creating empty request object");
            request = UpdateUserProfileRequest.builder().build();
        }
        
        // Handle image upload if provided
        if (image != null && !image.isEmpty()) {
            try {
                log.info("Processing image upload for userId: {}", userId);
                // Validate and upload image with thumbnail (32x32) and original (1024x1024)
                imageStorageService.validateImage(image);
                
                // Upload both thumbnail and original sizes
                ProfileImageResult imageResult = ((S3ImageStorageServiceImpl) imageStorageService)
                        .uploadImageWithThumbnail(image, userId.toString());
                
                // If request is null, create empty request
                if (request == null) {
                    request = UpdateUserProfileRequest.builder().build();
                }
                
                // Update profile with multi-size images
                try {
                    UserResponse response = userService.updateUserProfileWithMultiSizeImage(userId, request, imageResult);
                    log.debug("Successfully updated profile with multi-size image for userId: {}", userId);
                    return ResponseEntity.ok(ApiResponse.success("Profile updated successfully with image", response));
                } catch (Exception e) {
                    log.error("Error updating profile with image for userId: {}", userId, e);
                    throw e; // Re-throw to be handled by GlobalExceptionHandler
                }
            } catch (IllegalArgumentException e) {
                throw new BusinessException(e.getMessage());
            } catch (IOException e) {
                log.error("Failed to upload image for userId: {}", userId, e);
                throw new BusinessException("Failed to upload image: " + e.getMessage());
            }
        }
        
        // Regular profile update without image
        // If no image and no data provided, throw error
        // But allow empty JSON object "{}" to be sent (handled above)
        if (request == null) {
            throw new BusinessException("Either profile data or image must be provided. " +
                    "Send 'data' field with JSON (e.g., '{}' for empty update) or 'image' file, or both.");
        }
        
        try {
            UserResponse response = userService.updateUserProfile(userId, request);
            log.debug("Successfully updated profile for userId: {}, returning response", userId);
            return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
        } catch (Exception e) {
            log.error("Error updating profile for userId: {}", userId, e);
            throw e; // Re-throw to be handled by GlobalExceptionHandler
        }
    }

    /**
     * Gets the current user's token holding status.
     *
     * @param userId User ID from authentication principal
     * @return UserTokenHoldingStatus containing token balance and conversion information
     */
    @GetMapping("/me/status")
    @Operation(summary = "Get user token holding status", 
            description = "Retrieves the current user's token balance, level, conversion forecast, and related information")
    public ResponseEntity<ApiResponse<UserTokenHoldingStatus>> getUserTokenHoldingStatus(
            @AuthenticationPrincipal Long userId) {
        log.info("Getting token holding status for userId: {}", userId);
        UserTokenHoldingStatus status = userTokenStatusService.getUserTokenHoldingStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * Uploads a profile image for the current user.
     * 
     * <p>Generates both thumbnail (32x32px) and original (1024x1024px) versions:
     * <ul>
     *   <li>Thumbnail (~1-3KB) - for map markers, lists, avatars</li>
     *   <li>Original (~100-300KB) - for full profile view</li>
     * </ul>
     *
     * @param userId User ID from authentication principal
     * @param image Image file to upload
     * @return ImageUrlResponse containing the uploaded image URL (original size)
     */
    @PostMapping("/me/image")
    @Operation(summary = "Upload profile image", description = "Uploads a profile image for the current user. Generates multiple thumbnail sizes (32x32, 128x128, 512x512) and original (1024x1024) for optimal performance across various use cases.")
    public ResponseEntity<ApiResponse<ImageUrlResponse>> uploadProfileImage(
            @AuthenticationPrincipal Long userId,
            @RequestParam("image") MultipartFile image) {
        log.info("Uploading profile image for userId: {}", userId);

        try {
            // Validate and upload image with multiple thumbnails (32x32, 128x128, 512x512) and original (1024x1024)
            imageStorageService.validateImage(image);
            ProfileImageResult imageResult = ((S3ImageStorageServiceImpl) imageStorageService)
                    .uploadImageWithThumbnail(image, userId.toString());
            
            // Generate presigned URLs for all sizes and save to database
            String originalUrl = imageStorageService.getImageUrl(imageResult.getOriginalKey());
            String tinyUrl = imageStorageService.getImageUrl(imageResult.getTinyKey());
            String smallUrl = imageStorageService.getImageUrl(imageResult.getSmallKey());
            String mediumUrl = imageStorageService.getImageUrl(imageResult.getMediumKey());
            
            // Store all size URLs in database
            userService.updateProfileImageWithThumbnail(userId, originalUrl, tinyUrl, smallUrl, mediumUrl);

            return ResponseEntity.ok(ApiResponse.success(
                    new ImageUrlResponse(originalUrl)));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(e.getMessage());
        } catch (IOException e) {
            log.error("Failed to upload image for userId: {}", userId, e);
            throw new BusinessException("Failed to upload image: " + e.getMessage());
        }
    }

    /**
     * Gets the current user's profile image.
     *
     * @param userId User ID from authentication principal
     * @return Image binary data if image exists, or JSON with {"image": null} if no image
     */
    @GetMapping("/me/image")
    @Operation(summary = "Get current user's profile image", description = "Retrieves the current user's profile image. Returns JSON with {\"image\": null} if no image exists.")
    public ResponseEntity<?> getCurrentUserImage(
            @AuthenticationPrincipal Long userId) {
        log.info("Getting profile image for userId: {}", userId);

        try {
            UserResponse user = userService.getCurrentUser(userId);
            if (user.getProfileImageUrl() == null || user.getProfileImageUrl().trim().isEmpty()) {
                log.debug("User {} does not have a profile image URL set", userId);
                // Return JSON response with null image
                ImageResponse response = ImageResponse.builder().image(null).build();
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(response);
            }

            // Get stored value (could be key or URL for backward compatibility)
            // Note: user.getProfileImageUrl() now contains a fresh URL (generated in service layer)
            // But we still need to handle the case where we want to serve the image directly
            String storedValue = user.getProfileImageUrl();
            log.info("User {} profile image URL: {}", userId, storedValue);
            
            if (storedValue == null || storedValue.trim().isEmpty()) {
                // Return JSON response with null image
                ImageResponse response = ImageResponse.builder().image(null).build();
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(response);
            }
            
            // Since the response already has a fresh URL (generated from key), we can redirect to it
            // But first, try to extract the key to serve directly if needed, or redirect to URL
            String imageKey = ImageUrlHelper.extractImageKeyFromUrl(storedValue);
            
            // If we can extract a key, try to generate fresh URL and redirect
            if (imageKey != null) {
                try {
                    String freshImageUrl = imageStorageService.getImageUrl(imageKey);
                    if (freshImageUrl != null && (freshImageUrl.startsWith("http://") || freshImageUrl.startsWith("https://"))) {
                        log.info("Generated fresh image URL for userId: {}, redirecting", userId);
                        return ResponseEntity.status(HttpStatus.FOUND)
                                .header(HttpHeaders.LOCATION, freshImageUrl)
                                .build();
                    }
                } catch (Exception e) {
                    log.warn("Failed to generate URL from key: {} for userId: {}, using stored URL", 
                            imageKey, userId, e);
                }
            }
            
            // If it's already a URL, redirect to it
            if (storedValue.startsWith("http://") || storedValue.startsWith("https://")) {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header(HttpHeaders.LOCATION, storedValue)
                        .build();
            }
            
            // Fallback: try to serve image bytes directly
            if (imageKey != null) {
                try {
                    byte[] imageBytes;
                    if (imageKey.startsWith("users/")) {
                        // Full S3 key format
                        imageBytes = imageStorageService.getImageBytes(imageKey);
                    } else {
                        // Just filename, use userId
                        imageBytes = imageStorageService.getImageBytes(userId.toString(), imageKey);
                    }
                    log.info("Successfully retrieved image for userId: {}, imageKey: {}, size: {} bytes", 
                            userId, imageKey, imageBytes.length);
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE)
                            .body(imageBytes);
                } catch (IOException e) {
                    log.error("Failed to retrieve image file for userId: {}, imageKey: {}, error: {}", 
                            userId, imageKey, e.getMessage(), e);
                }
            }
            
            // Final fallback: return null image
            ImageResponse response = ImageResponse.builder().image(null).build();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
        } catch (Exception e) {
            log.error("Unexpected error retrieving image for userId: {}", userId, e);
            // Return JSON response with null image on unexpected error
            ImageResponse response = ImageResponse.builder().image(null).build();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
        }
    }

    /**
     * Gets a user's profile image by user ID (public endpoint).
     * 
     * <p>Note: This endpoint retrieves the profile image for a specific user.
     * Returns JSON with {"image": null} if no image exists.
     *
     * @param userId User ID
     * @return Image binary data if image exists, or JSON with {"image": null} if no image
     */
    @GetMapping("/{userId}/image")
    @Operation(summary = "Get user profile image", description = "Retrieves a user's profile image by user ID. Returns JSON with {\"image\": null} if no image exists.")
    public ResponseEntity<?> getUserImage(
            @PathVariable Long userId) {
        log.info("Getting profile image for userId: {}", userId);

        try {
            // Use query service for reads (CQRS pattern)
            UserResponse user = userQueryService.getUserById(userId);
            if (user.getProfileImageUrl() == null || user.getProfileImageUrl().trim().isEmpty()) {
                log.debug("User {} does not have a profile image URL set", userId);
                // Return JSON response with null image
                ImageResponse response = ImageResponse.builder().image(null).build();
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(response);
            }

            // Get image URL (already enriched with fresh URL by UserQueryService)
            String imageUrl = user.getProfileImageUrl();
            log.info("User {} profile image URL: {}", userId, imageUrl);
            
            // If it's a URL, redirect to it (fresh URL generated by service layer)
            if (imageUrl != null && (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))) {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header(HttpHeaders.LOCATION, imageUrl)
                        .build();
            }
            
            // If not a URL, try to extract key and generate URL
            String imageKey = ImageUrlHelper.extractImageKeyFromUrl(imageUrl);
            if (imageKey != null) {
                try {
                    String freshImageUrl = imageStorageService.getImageUrl(imageKey);
                    if (freshImageUrl != null && (freshImageUrl.startsWith("http://") || freshImageUrl.startsWith("https://"))) {
                        return ResponseEntity.status(HttpStatus.FOUND)
                                .header(HttpHeaders.LOCATION, freshImageUrl)
                                .build();
                    }
                } catch (Exception e) {
                    log.warn("Failed to generate URL from key: {} for userId: {}, trying to serve bytes", 
                            imageKey, userId, e);
                }
                
                // Fallback: serve image bytes directly
                try {
                    byte[] imageBytes;
                    if (imageKey.startsWith("users/")) {
                        imageBytes = imageStorageService.getImageBytes(imageKey);
                    } else {
                        imageBytes = imageStorageService.getImageBytes(userId.toString(), imageKey);
                    }
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE)
                            .body(imageBytes);
                } catch (IOException e) {
                    log.error("Failed to retrieve image file for userId: {}, imageKey: {}", userId, imageKey, e);
                }
            }
            
            // Final fallback: return null image
            ImageResponse response = ImageResponse.builder().image(null).build();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
        } catch (ResourceNotFoundException e) {
            log.warn("User not found: {}", userId);
            // Return JSON response with null image when user not found
            ImageResponse response = ImageResponse.builder().image(null).build();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
        }
    }

    /**
     * Gets a user's profile image by user ID and filename (public endpoint).
     * 
     * <p>Note: This endpoint serves images from the stored image URL.
     * The image URL format is: /api/v1/users/{userId}/image/{filename}
     *
     * @param userId User ID
     * @param filename Image filename
     * @return Image binary data
     */
    @GetMapping("/{userId}/image/{filename}")
    @Operation(summary = "Get user profile image by filename", description = "Retrieves a user's profile image by user ID and filename (public endpoint)")
    public ResponseEntity<byte[]> getUserImageByFilename(
            @PathVariable Long userId,
            @PathVariable String filename) {
        log.info("Getting profile image for userId: {}, filename: {}", userId, filename);

        try {
            // Use filename as imageKey
            byte[] imageBytes = imageStorageService.getImageBytes(filename);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE)
                    .body(imageBytes);
        } catch (IOException e) {
            log.error("Failed to retrieve image for userId: {}, filename: {}", userId, filename, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Changes the current user's password.
     *
     * @param userId User ID from authentication principal
     * @param request Change password request containing old and new password
     * @return Success message
     */
    @PostMapping("/me/change-password")
    @Operation(summary = "Change password", description = "Changes the current user's password")
    public ResponseEntity<ApiResponse<MessageResponse>> changePassword(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        log.info("Changing password for userId: {}", userId);

        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());

        return ResponseEntity.ok(ApiResponse.success(
                MessageResponse.builder()
                        .message("Password changed successfully")
                        .build()));
    }

    /**
     * Sends email verification code to the authenticated user's email.
     *
     * <p>This endpoint sends a verification code to the user's registered email address.
     * The user must be authenticated to use this endpoint.
     *
     * @param userId User ID from authentication principal
     * @return Success message
     */
    @PostMapping("/me/email/send")
    @Operation(
            summary = "Send email verification code to profile",
            description = "Sends a 4-digit verification code to the authenticated user's email address. Rate limit: 3 requests/hour."
    )
    public ResponseEntity<ApiResponse<MessageResponse>> sendProfileVerificationEmail(
            @AuthenticationPrincipal Long userId) {
        log.info("Profile email verification request for userId: {}", userId);

        emailVerificationService.sendProfileVerificationCode(userId);
        
        // Get user email to include in response
        String userEmail = userService.getUserById(userId).getEmail();

        MessageResponse response = MessageResponse.builder()
                .message("Email verification code sent successfully")
                .verificationMethod("Email")
                .verificationType(2) // Email verification type
                .email(userEmail) // Include email address when sent via email
                .sentTo(userEmail)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Verifies email code for the authenticated user.
     *
     * <p>This endpoint verifies the email code and updates the user's emailVerified status.
     * The user must be authenticated to use this endpoint.
     *
     * @param userId User ID from authentication principal
     * @param request Email verification request
     * @return Success message
     */
    @PostMapping("/me/email/verify")
    @Operation(
            summary = "Verify email code for profile",
            description = "Verifies the email verification code sent to the authenticated user's email address."
    )
    public ResponseEntity<ApiResponse<MessageResponse>> verifyProfileEmail(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody VerifyEmailRequest request) {
        log.info("Profile email verification request for userId: {}", userId);

        // Verify the code
        emailVerificationService.verifyEmailCode(request.getEmail(), request.getCode());

        MessageResponse response = MessageResponse.builder()
                .message("Email verified successfully")
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Block a user
     * 
     * @param blockerUserId User ID from authentication
     * @param userId User ID to block
     * @return Success message
     */
    @PostMapping("/{userId}/block")
    @Operation(summary = "Block user", description = "Block a user to prevent seeing their profile on the map.")
    public ResponseEntity<ApiResponse<MessageResponse>> blockUser(
            @AuthenticationPrincipal Long blockerUserId,
            @PathVariable Long userId) {
        log.info("Block request: user {} blocking user {}", blockerUserId, userId);
        userBlockService.blockUser(blockerUserId, userId);
        MessageResponse response = MessageResponse.builder()
                .message("User blocked successfully")
                .build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Unblock a user
     * 
     * @param blockerUserId User ID from authentication
     * @param userId User ID to unblock
     * @return Success message
     */
    @DeleteMapping("/{userId}/block")
    @Operation(summary = "Unblock user", description = "Unblock a previously blocked user.")
    public ResponseEntity<ApiResponse<MessageResponse>> unblockUser(
            @AuthenticationPrincipal Long blockerUserId,
            @PathVariable Long userId) {
        log.info("Unblock request: user {} unblocking user {}", blockerUserId, userId);
        userBlockService.unblockUser(blockerUserId, userId);
        MessageResponse response = MessageResponse.builder()
                .message("User unblocked successfully")
                .build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get list of blocked users
     * 
     * @param userId User ID from authentication
     * @return List of blocked users
     */
    @GetMapping("/me/blocks")
    @Operation(summary = "Get blocked users", description = "Get list of users you have blocked.")
    public ResponseEntity<ApiResponse<List<BlockedUserResponse>>> getBlockedUsers(
            @AuthenticationPrincipal Long userId) {
        log.info("Getting blocked users for user: {}", userId);
        List<BlockedUserResponse> blockedUsers = userBlockService.getBlockedUsers(userId);
        return ResponseEntity.ok(ApiResponse.success(blockedUsers));
    }

    /**
     * Report a user
     * 
     * @param reporterUserId User ID from authentication
     * @param userId User ID to report
     * @param request Report request
     * @return Report response
     */
    @PostMapping("/{userId}/report")
    @Operation(summary = "Report user", description = "Report a user for vulnerable/illegal activity")
    public ResponseEntity<ApiResponse<UserReportResponse>> reportUser(
            @AuthenticationPrincipal Long reporterUserId,
            @PathVariable Long userId,
            @Valid @RequestBody CreateUserReportRequest request) {
        log.info("Report request: user {} reporting user {}", reporterUserId, userId);
        UserReportResponse response = userReportService.createReport(reporterUserId, userId, request);
        return ResponseEntity.ok(ApiResponse.success("Report submitted successfully", response));
    }

    /**
     * Get my reports
     * 
     * @param userId User ID from authentication
     * @return List of reports
     */
    @GetMapping("/me/reports")
    @Operation(summary = "Get my reports", description = "Get list of reports I have submitted")
    public ResponseEntity<ApiResponse<List<UserReportResponse>>> getMyReports(
            @AuthenticationPrincipal Long userId) {
        log.info("Getting reports for user: {}", userId);
        List<UserReportResponse> reports = userReportService.getMyReports(userId);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    /**
     * Get report by ID
     * 
     * @param userId User ID from authentication
     * @param reportId Report ID
     * @return Report response
     */
    @GetMapping("/reports/{reportId}")
    @Operation(summary = "Get report details", description = "Get details of a specific report")
    public ResponseEntity<ApiResponse<UserReportResponse>> getReportById(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long reportId) {
        log.info("Getting report {} for user: {}", reportId, userId);
        UserReportResponse response = userReportService.getReportById(reportId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Deletes the current authenticated user's account.
     * @param userId User ID from authentication principal
     * @return Success message
     */
    @DeleteMapping("/me")
    @Operation(summary = "Delete my account", 
            description = "Deletes the current authenticated user's account (soft delete). This action is permanent and cannot be undone by the user.")
    public ResponseEntity<ApiResponse<String>> deleteMyAccount(
            @AuthenticationPrincipal Long userId) {
        log.info("Account deletion request received for userId: {}", userId);
        
        userService.deleteMyAccount(userId);
        
        return ResponseEntity.ok(ApiResponse.success("Account deleted successfully"));
    }


}

