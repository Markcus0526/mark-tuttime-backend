package com.tuttime.user.controller;

import com.tuttime.common.dto.ApiResponse;
import com.tuttime.common.exception.AuthenticationException;
import com.tuttime.user.service.LanguageService;
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

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Set;

/**
 * REST Controller for Language Management Operations
 *
 * <p>This controller handles HTTP requests related to language translations,
 * providing endpoints to retrieve available languages and their translations.
 * Also provides admin endpoints to upload/update/delete language files.
 *
 * <p><b>Public Endpoints:</b>
 * <ul>
 *   <li>GET /api/v1/users/languages - Get list of available language codes</li>
 *   <li>GET /api/v1/users/languages/{langCode} - Get translations for a specific language</li>
 *   <li>GET /api/v1/users/languages/all - Get all language translations</li>
 * </ul>
 *
 * <p><b>Admin Endpoints:</b>
 * <ul>
 *   <li>POST /api/v1/users/languages/{langCode} - Upload/update language translations</li>
 *   <li>DELETE /api/v1/users/languages/{langCode} - Delete a language file</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users/languages")
@RequiredArgsConstructor
@Tag(name = "Language Management", description = "Language translation APIs")
public class LanguageController {

    private final LanguageService languageService;

    /**
     * Get list of available language codes
     *
     * @return ResponseEntity containing set of available language codes
     */
    @GetMapping
    @Operation(summary = "Get available languages", 
            description = "Returns a list of all supported language codes")
    public ResponseEntity<ApiResponse<Set<String>>> getAvailableLanguages() {
        log.debug("Getting available languages");
        Set<String> languages = languageService.getAvailableLanguages();
        return ResponseEntity.ok(ApiResponse.success("Available languages retrieved successfully", languages));
    }

    /**
     * Get language translations for a specific language code
     *
     * @param langCode Language code (e.g., "en", "ru", "zh")
     * @param request HTTP request to check for cache bypass header
     * @return ResponseEntity containing translations map for the specified language
     */
    @GetMapping("/{langCode}")
    @Operation(summary = "Get language translations", 
            description = "Returns all translations for the specified language code. Add 'X-No-Cache: true' header to bypass cache.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLanguageTranslations(
            @PathVariable String langCode,
            HttpServletRequest request) {
        boolean skipCache = isNoCacheRequest(request);
        log.debug("Getting language translations for code: {} (skipCache: {})", langCode, skipCache);
        Map<String, Object> translations = languageService.getLanguageTranslations(langCode, skipCache);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success("Language translations retrieved successfully", translations));
    }

    /**
     * Get all language translations (all languages)
     *
     * @param request HTTP request to check for cache bypass header
     * @return ResponseEntity containing map of all language translations
     */
    @GetMapping("/all")
    @Operation(summary = "Get all language translations", 
            description = "Returns translations for all supported languages. Add 'X-No-Cache: true' header to bypass cache.")
    public ResponseEntity<ApiResponse<Map<String, Map<String, Object>>>> getAllLanguageTranslations(
            HttpServletRequest request) {
        boolean skipCache = isNoCacheRequest(request);
        log.debug("Getting all language translations (skipCache: {})", skipCache);
        Map<String, Map<String, Object>> allTranslations = languageService.getAllLanguageTranslations(skipCache);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success("All language translations retrieved successfully", allTranslations));
    }

    /**
     * Upload or update language translations (Admin only)
     * 
     * @param langCode Language code (e.g., "en", "ko")
     * @param translations Map of translation key-value pairs
     * @return ResponseEntity with success message
     */
    @PostMapping("/{langCode}")
    @Operation(summary = "Upload/update language translations", 
            description = "Uploads or updates translations for a language. Overwrites existing file.")
    public ResponseEntity<ApiResponse<Void>> uploadTranslations(
            @PathVariable String langCode,
            @Valid @RequestBody Map<String, Object> translations) {
        log.info("Uploading translations for language: {}", langCode);
        languageService.uploadTranslations(langCode, translations);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Language translations uploaded successfully", null));
    }

    /**
     * Update language translations (Admin only)
     * Same as POST but uses PUT for semantic clarity
     * 
     * @param langCode Language code (e.g., "en", "ko")
     * @param translations Map of translation key-value pairs
     * @return ResponseEntity with success message
     */
    @PutMapping("/{langCode}")
    @Operation(summary = "Update language translations", 
            description = "Updates translations for a language. Overwrites existing file.")
    public ResponseEntity<ApiResponse<Void>> updateTranslations(
            @PathVariable String langCode,
            @Valid @RequestBody Map<String, Object> translations) {
        log.info("Updating translations for language: {}", langCode);
        languageService.uploadTranslations(langCode, translations);
        return ResponseEntity.ok()
                .body(ApiResponse.success("Language translations updated successfully", null));
    }

    /**
     * Delete a language file (Admin only)
     * 
     * @param langCode Language code to delete
     * @return ResponseEntity with success message
     */
    @DeleteMapping("/{langCode}")
    @Operation(summary = "Delete language", 
            description = "Deletes a language file and all its translations")
    public ResponseEntity<ApiResponse<Void>> deleteLanguage(@PathVariable String langCode) {
        log.info("Deleting language: {}", langCode);
        languageService.deleteLanguage(langCode);
        return ResponseEntity.ok()
                .body(ApiResponse.success("Language deleted successfully", null));
    }

    /**
     * Get apple language translations for authenticated users (post-signin)
     * 
     * <p>This endpoint requires authentication and checks if the user ID matches 
     * the configured apple user ID. If it matches, returns translations from 
     * jsons/apple-languages/ prefix in S3. Otherwise, returns normal language translations.
     *
     * @param langCode Language code (e.g., "en", "ru", "zh")
     * @param userId Authenticated user ID (from JWT token)
     * @return ResponseEntity containing translations map for the specified language
     */
    @GetMapping("/apple/{langCode}")
    @Operation(summary = "Get apple language translations (Authenticated)", 
            description = "Returns translations for the specified language code. If userId matches configured appleUserId, returns apple-languages set, otherwise returns normal languages. Requires authentication. Add 'X-No-Cache: true' header to bypass cache.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAppleLanguageTranslations(
            @PathVariable String langCode,
            @AuthenticationPrincipal Long userId,
            HttpServletRequest request) {
        if (userId == null) {
            throw new AuthenticationException("User ID not found in authentication token");
        }
        boolean skipCache = isNoCacheRequest(request);
        log.info("Getting language translations for code: {} for user: {} (checking if apple user, skipCache: {})", 
                langCode, userId, skipCache);
        Map<String, Object> translations = languageService.getAppleLanguageTranslations(langCode, userId, skipCache);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success("Language translations retrieved successfully", translations));
    }

    /**
     * Get all apple language translations for authenticated users (post-signin)
     * 
     * <p>This endpoint requires authentication and checks if the user ID matches 
     * the configured apple user ID. If it matches, returns all translations from 
     * jsons/apple-languages/ prefix in S3. Otherwise, returns all normal language translations.
     *
     * @param userId Authenticated user ID (from JWT token)
     * @return ResponseEntity containing map of all language translations
     */
    @GetMapping("/apple/all")
    @Operation(summary = "Get all apple language translations (Authenticated)", 
            description = "Returns translations for all available languages. If userId matches configured appleUserId, returns apple-languages set, otherwise returns normal languages. Requires authentication. Add 'X-No-Cache: true' header to bypass cache.")
    public ResponseEntity<ApiResponse<Map<String, Map<String, Object>>>> getAllAppleLanguageTranslations(
            @AuthenticationPrincipal Long userId,
            HttpServletRequest request) {
        if (userId == null) {
            throw new AuthenticationException("User ID not found in authentication token");
        }
        boolean skipCache = isNoCacheRequest(request);
        log.debug("Getting all language translations for user: {} (checking if apple user, skipCache: {})", 
                userId, skipCache);
        Map<String, Map<String, Object>> allTranslations = languageService.getAllAppleLanguageTranslations(userId, skipCache);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success("All language translations retrieved successfully", allTranslations));
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

