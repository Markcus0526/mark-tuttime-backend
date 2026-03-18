package com.tuttime.user.service;

import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Policy service for loading and serving policy files from S3
 * Supports English (en) and Russian (ru) languages
 */
@Slf4j
@Service
public class PolicyService {

    private static final String S3_POLICIES_PREFIX = "jsons/policies/";
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("en", "ru");
    
    private final ObjectMapper objectMapper;
    private final Map<String, Map<String, Object>> policyCache = new HashMap<>();
    
    @Value("${app.image.s3.endpoint:https://s3.ru-7.storage.selcloud.ru}")
    private String s3Endpoint;

    @Value("${app.image.s3.region:ru-7}")
    private String s3Region;

    @Value("${app.image.s3.bucket:tuttime-storage}")
    private String bucketName;

    @Value("${app.image.s3.access-key:}")
    private String accessKey;

    @Value("${app.image.s3.secret-key:}")
    private String secretKey;

    private S3Client s3Client;

    public PolicyService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    private void initializeS3Client() {
        if (accessKey == null || accessKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
            throw new BusinessException("S3 credentials not configured");
        }
        
        try {
            s3Client = S3Client.builder()
                    .endpointOverride(URI.create(s3Endpoint))
                    .region(Region.of(s3Region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .forcePathStyle(true)
                    .build();
        } catch (Exception e) {
            throw new BusinessException("Failed to initialize S3 client: " + e.getMessage(), e);
        }
    }

    private S3Client getS3Client() {
        if (s3Client == null) {
            throw new BusinessException("S3 client not initialized");
        }
        return s3Client;
    }

    /**
     * Get list of available policy language codes
     * Only returns supported languages (en, ru)
     *
     * @return Set of available language codes
     */
    public Set<String> getAvailableLanguages() {
        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(S3_POLICIES_PREFIX)
                    .build();
            
            ListObjectsV2Response listResponse = getS3Client().listObjectsV2(listRequest);
            
            Set<String> languages = listResponse.contents().stream()
                    .filter(s3Object -> s3Object.key().endsWith(".json"))
                    .map(s3Object -> {
                        String fileName = s3Object.key().substring(S3_POLICIES_PREFIX.length());
                        return fileName.substring(0, fileName.length() - 5).toLowerCase();
                    })
                    .filter(SUPPORTED_LANGUAGES::contains)
                    .collect(Collectors.toSet());
            
            return languages.isEmpty() ? new HashSet<>(SUPPORTED_LANGUAGES) : languages;
        } catch (Exception e) {
            log.warn("Failed to list policy files from S3, returning default supported languages: {}", e.getMessage());
            return new HashSet<>(SUPPORTED_LANGUAGES);
        }
    }

    /**
     * Get policy content for a specific language code
     *
     * @param langCode Language code (e.g., "en", "ru")
     * @return Map containing policy content
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getPolicy(String langCode) {
        langCode = langCode.toLowerCase();
        
        // Validate language code
        if (!SUPPORTED_LANGUAGES.contains(langCode)) {
            throw new BusinessException("Unsupported language code: " + langCode + ". Supported languages: " + SUPPORTED_LANGUAGES);
        }

        // Check cache first
        if (policyCache.containsKey(langCode)) {
            return policyCache.get(langCode);
        }

        String s3Key = S3_POLICIES_PREFIX + langCode + ".json";
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            try (InputStream inputStream = getS3Client().getObject(getObjectRequest)) {
                Map<String, Object> policy = objectMapper.readValue(inputStream, Map.class);
                policyCache.put(langCode, policy);
                return policy;
            }
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFoundException("Policy file not found for language: " + langCode);
        } catch (Exception e) {
            throw new BusinessException("Failed to load policy file: " + e.getMessage(), e);
        }
    }

    /**
     * Get all policy translations (all supported languages)
     *
     * @return Map of language code to policy content
     */
    public Map<String, Map<String, Object>> getAllPolicies() {
        Map<String, Map<String, Object>> allPolicies = new HashMap<>();
        for (String langCode : getAvailableLanguages()) {
            try {
                allPolicies.put(langCode, getPolicy(langCode));
            } catch (ResourceNotFoundException e) {
                // Skip missing policies
                log.debug("Policy not found for language: {}", langCode);
            }
        }
        return allPolicies;
    }

    /**
     * Clear policy cache
     */
    public void clearCache() {
        policyCache.clear();
    }

    /**
     * Clear cache for specific language
     */
    public void clearCache(String langCode) {
        policyCache.remove(langCode.toLowerCase());
    }
}

