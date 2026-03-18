package com.tuttime.admin.controller;

import com.tuttime.admin.dto.LanguageResponse;
import com.tuttime.admin.entity.AdminAuditLog;
import com.tuttime.admin.service.AdminAuditService;
import com.tuttime.admin.service.AdminLanguageService;
import com.tuttime.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin Language Management Controller
 * 
 * <p>Provides endpoints for managing languages:
 * <ul>
 *   <li>GET /api/admin/orchestration/languages - Get all languages</li>
 *   <li>GET /api/admin/orchestration/languages/{code} - Get language translations by code</li>
 *   <li>POST /api/admin/orchestration/languages - Upload new language (JSON file)</li>
 *   <li>PUT /api/admin/orchestration/languages/{code} - Update language (upload new JSON file)</li>
 *   <li>DELETE /api/admin/orchestration/languages/{code} - Delete language</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/orchestration/languages")
@RequiredArgsConstructor
@Tag(name = "Admin Language Management", description = "Language and translation management APIs")
public class AdminLanguageController {

    private final AdminLanguageService languageService;
    private final AdminAuditService auditService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "List all languages", description = "Get list of all supported languages")
    public ResponseEntity<ApiResponse<List<LanguageResponse>>> listLanguages(
            @AuthenticationPrincipal Long adminId) {
        
        List<LanguageResponse> languages = languageService.listLanguages();
        
        auditService.logAction(adminId, null,
                AdminAuditLog.AdminActionType.CONFIG_UPDATED,
                "LANGUAGES", null, "Listed languages");
        
        return ResponseEntity.ok(ApiResponse.success("Languages retrieved successfully", languages));
    }

    @GetMapping("/{code}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get language translations", description = "Get language translations by language code")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLanguage(
            @PathVariable String code,
            @AuthenticationPrincipal Long adminId) {
        
        // Get translations directly from user-service (preserves nested structure)
        Map<String, Object> translations = languageService.getLanguageTranslations(code);
        
        auditService.logAction(adminId, null,
                AdminAuditLog.AdminActionType.CONFIG_UPDATED,
                "LANGUAGES", null, "Viewed language translations: " + code);
        
        return ResponseEntity.ok(ApiResponse.success("Language translations retrieved successfully", translations));
    }

    @PostMapping(consumes = "application/json")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Upload new language", description = "Upload a new language JSON file. Request body should contain 'code' field and the translations JSON.")
    public ResponseEntity<ApiResponse<Void>> createLanguage(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal Long adminId) {
        
        log.debug("Received language upload request with keys: {}", request.keySet());
        
        if (request == null || request.isEmpty()) {
            throw new IllegalArgumentException("Request body is required");
        }
        
        // Extract language code from request body
        String languageCode = null;
        if (request.containsKey("code")) {
            languageCode = request.get("code").toString();
            request.remove("code"); // Remove code from translations
        } else if (request.containsKey("languageCode")) {
            languageCode = request.get("languageCode").toString();
            request.remove("languageCode"); // Remove languageCode from translations
        }
        
        if (languageCode == null || languageCode.isEmpty()) {
            throw new IllegalArgumentException("Language code is required in request body (use 'code' or 'languageCode' field)");
        }
        
        log.info("Uploading language: {} with {} translation keys", languageCode, request.size());
        
        // The rest of the request body is the translations JSON
        languageService.uploadLanguage(languageCode, request, adminId);
        
        auditService.logAction(adminId, null,
                AdminAuditLog.AdminActionType.LANGUAGE_CREATED,
                "LANGUAGES", null, "Uploaded new language: " + languageCode + ", translation keys: " + request.size());
        
        return ResponseEntity.ok(ApiResponse.success("Language uploaded successfully", null));
    }

    @PutMapping("/{code}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Update language", description = "Update language by uploading new JSON file")
    public ResponseEntity<ApiResponse<Void>> updateLanguage(
            @PathVariable String code,
            @RequestBody Map<String, Object> translations,
            @AuthenticationPrincipal Long adminId) {
        
        languageService.uploadLanguage(code, translations, adminId);
        
        auditService.logAction(adminId, null,
                AdminAuditLog.AdminActionType.LANGUAGE_UPDATED,
                "LANGUAGES", null, "Updated language: " + code + ", keys: " + translations.size());
        
        return ResponseEntity.ok(ApiResponse.success("Language updated successfully", null));
    }

    @DeleteMapping("/{code}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Delete language", description = "Delete a language file")
    public ResponseEntity<ApiResponse<Void>> deleteLanguage(
            @PathVariable String code,
            @AuthenticationPrincipal Long adminId) {
        
        languageService.deleteLanguage(code);
        
        auditService.logAction(adminId, null,
                AdminAuditLog.AdminActionType.LANGUAGE_DELETED,
                "LANGUAGES", null, "Deleted language: " + code);
        
        return ResponseEntity.ok(ApiResponse.success("Language deleted successfully", null));
    }
}

