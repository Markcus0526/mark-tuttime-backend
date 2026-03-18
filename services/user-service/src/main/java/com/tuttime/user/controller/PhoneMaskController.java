package com.tuttime.user.controller;

import com.tuttime.common.dto.ApiResponse;
import com.tuttime.user.service.PhoneMaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Phone Mask Management Operations
 *
 * <p>This controller handles HTTP requests related to phone masks,
 * providing endpoints to retrieve phone mask information for different countries.
 *
 * <p><b>Public Endpoints:</b>
 * <ul>
 *   <li>GET /api/v1/users/phone-masks - Get all phone masks</li>
 *   <li>GET /api/v1/users/phone-masks/iso/{isoCode} - Get phone mask by ISO code</li>
 *   <li>GET /api/v1/users/phone-masks/code/{phoneCode} - Get phone mask by phone code</li>
 * </ul>
 *
 * <p><b>Admin Endpoints:</b>
 * <ul>
 *   <li>POST /api/v1/users/phone-masks - Upload/update phone masks</li>
 *   <li>PUT /api/v1/users/phone-masks - Update phone masks</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users/phone-masks")
@RequiredArgsConstructor
@Tag(name = "Phone Mask Management", description = "Phone mask APIs")
public class PhoneMaskController {

    private final PhoneMaskService phoneMaskService;

    /**
     * Get all phone masks
     *
     * @param request HTTP request to check for cache bypass header
     * @return ResponseEntity containing list of all phone masks
     */
    @GetMapping
    @Operation(summary = "Get all phone masks", 
            description = "Returns all phone masks for all countries. Add 'X-No-Cache: true' header to bypass cache.")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllPhoneMasks(
            HttpServletRequest request) {
        boolean skipCache = isNoCacheRequest(request);
        log.debug("Getting all phone masks (skipCache: {})", skipCache);
        List<Map<String, Object>> phoneMasks = phoneMaskService.getPhoneMasks(skipCache);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success("Phone masks retrieved successfully", phoneMasks));
    }

    /**
     * Get phone mask by ISO code
     *
     * @param isoCode Country ISO code (e.g., "US", "RU", "GB")
     * @param request HTTP request to check for cache bypass header
     * @return ResponseEntity containing phone mask for the specified country
     */
    @GetMapping("/iso/{isoCode}")
    @Operation(summary = "Get phone mask by ISO code", 
            description = "Returns phone mask for the specified country ISO code. Add 'X-No-Cache: true' header to bypass cache.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPhoneMaskByIso(
            @PathVariable String isoCode,
            HttpServletRequest request) {
        boolean skipCache = isNoCacheRequest(request);
        log.debug("Getting phone mask for ISO code: {} (skipCache: {})", isoCode, skipCache);
        Map<String, Object> phoneMask = phoneMaskService.getPhoneMaskByIso(isoCode, skipCache);
        if (phoneMask == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Phone mask not found for ISO code: " + isoCode));
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success("Phone mask retrieved successfully", phoneMask));
    }

    /**
     * Get phone mask by phone code (country calling code)
     *
     * @param phoneCode Country calling code (e.g., "+1", "+7", "+44")
     * @param request HTTP request to check for cache bypass header
     * @return ResponseEntity containing phone mask for the specified country
     */
    @GetMapping("/code/{phoneCode}")
    @Operation(summary = "Get phone mask by phone code", 
            description = "Returns phone mask for the specified country calling code. Add 'X-No-Cache: true' header to bypass cache.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPhoneMaskByCode(
            @PathVariable String phoneCode,
            HttpServletRequest request) {
        boolean skipCache = isNoCacheRequest(request);
        log.debug("Getting phone mask for phone code: {} (skipCache: {})", phoneCode, skipCache);
        Map<String, Object> phoneMask = phoneMaskService.getPhoneMaskByCode(phoneCode, skipCache);
        if (phoneMask == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Phone mask not found for phone code: " + phoneCode));
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success("Phone mask retrieved successfully", phoneMask));
    }

    /**
     * Upload or update phone masks (Admin only)
     * 
     * @param phoneMasks List of phone mask objects
     * @return ResponseEntity with success message
     */
    @PostMapping
    @Operation(summary = "Upload/update phone masks", 
            description = "Uploads or updates phone masks. Overwrites existing file.")
    public ResponseEntity<ApiResponse<Void>> uploadPhoneMasks(
            @Valid @RequestBody List<Map<String, Object>> phoneMasks) {
        log.info("Uploading phone masks");
        phoneMaskService.uploadPhoneMasks(phoneMasks);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Phone masks uploaded successfully", null));
    }

    /**
     * Update phone masks (Admin only)
     * Same as POST but uses PUT for semantic clarity
     * 
     * @param phoneMasks List of phone mask objects
     * @return ResponseEntity with success message
     */
    @PutMapping
    @Operation(summary = "Update phone masks", 
            description = "Updates phone masks. Overwrites existing file.")
    public ResponseEntity<ApiResponse<Void>> updatePhoneMasks(
            @Valid @RequestBody List<Map<String, Object>> phoneMasks) {
        log.info("Updating phone masks");
        phoneMaskService.uploadPhoneMasks(phoneMasks);
        return ResponseEntity.ok()
                .body(ApiResponse.success("Phone masks updated successfully", null));
    }

    /**
     * Check if the request has cache bypass header
     * Supports both 'X-No-Cache: true' and 'Cache-Control: no-cache' headers
     *
     * @param request HTTP request
     * @return true if cache should be bypassed
     */
    private boolean isNoCacheRequest(HttpServletRequest request) {
        String xNoCache = request.getHeader("X-No-Cache");
        String cacheControl = request.getHeader("Cache-Control");
        
        // Check X-No-Cache header
        if (xNoCache != null && ("true".equalsIgnoreCase(xNoCache) || "1".equals(xNoCache))) {
            return true;
        }
        
        // Check Cache-Control header
        if (cacheControl != null && cacheControl.toLowerCase().contains("no-cache")) {
            return true;
        }
        
        return false;
    }
}

