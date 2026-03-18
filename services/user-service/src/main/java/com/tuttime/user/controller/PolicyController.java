package com.tuttime.user.controller;

import com.tuttime.common.dto.ApiResponse;
import com.tuttime.user.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * REST Controller for Policy Management Operations
 *
 * <p>This controller handles HTTP requests related to policy documents,
 * providing endpoints to retrieve policies in supported languages (English and Russian).
 *
 * <p><b>Public Endpoints:</b>
 * <ul>
 *   <li>GET /api/v1/users/policies - Get list of available policy language codes</li>
 *   <li>GET /api/v1/users/policies/{langCode} - Get policy for a specific language</li>
 *   <li>GET /api/v1/users/policies/all - Get all policy translations</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users/policies")
@RequiredArgsConstructor
@Tag(name = "Policy Management", description = "Policy document APIs")
public class PolicyController {

    private final PolicyService policyService;

    /**
     * Get list of available policy language codes
     *
     * @return ResponseEntity containing set of available language codes
     */
    @GetMapping
    @Operation(summary = "Get available policy languages", 
            description = "Returns a list of all supported policy language codes (en, ru)")
    public ResponseEntity<ApiResponse<Set<String>>> getAvailableLanguages() {
        log.debug("Getting available policy languages");
        Set<String> languages = policyService.getAvailableLanguages();
        return ResponseEntity.ok(ApiResponse.success("Available policy languages retrieved successfully", languages));
    }

    /**
     * Get policy content for a specific language code
     *
     * @param langCode Language code (e.g., "en", "ru")
     * @return ResponseEntity containing policy content for the specified language
     */
    @GetMapping("/{langCode}")
    @Operation(summary = "Get policy by language", 
            description = "Returns policy content for the specified language code (en or ru)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPolicy(
            @PathVariable String langCode) {
        log.debug("Getting policy for language code: {}", langCode);
        Map<String, Object> policy = policyService.getPolicy(langCode);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success("Policy retrieved successfully", policy));
    }

    /**
     * Get all policy translations (all supported languages)
     *
     * @return ResponseEntity containing map of all policy translations
     */
    @GetMapping("/all")
    @Operation(summary = "Get all policy translations", 
            description = "Returns policies for all supported languages (en, ru)")
    public ResponseEntity<ApiResponse<Map<String, Map<String, Object>>>> getAllPolicies() {
        log.debug("Getting all policy translations");
        Map<String, Map<String, Object>> allPolicies = policyService.getAllPolicies();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success("All policy translations retrieved successfully", allPolicies));
    }
}





