package com.tuttime.user.service;

import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/** Language service for loading and serving translation files from S3 */
@Slf4j
@Service
public class LanguageService {

    private static final String S3_LANGUAGES_PREFIX = "jsons/languages/";
    private static final String S3_APPLE_LANGUAGES_PREFIX = "jsons/apple-languages/";
    
    private final ObjectMapper objectMapper;
    private final Map<String, Map<String, Object>> languageCache = new HashMap<>();
    private final Map<String, Map<String, Object>> appleLanguageCache = new HashMap<>();
    
    @Value("${app.image.s3.endpoint:https://s3.ru-7.storage.selcloud.ru}")
    private String s3Endpoint;

    @Value("${app.image.s3.region:ru-7}")
    private String s3Region;

    @Value("${app.image.s3.bucket:boostx-storage}")
    private String bucketName;

    @Value("${app.image.s3.access-key:}")
    private String accessKey;

    @Value("${app.image.s3.secret-key:}")
    private String secretKey;

    @Value("${app.language.apple-user-id:}")
    private Long appleUserId;

    private S3Client s3Client;

    public LanguageService(ObjectMapper objectMapper) {
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

    public Set<String> getAvailableLanguages() {
        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(S3_LANGUAGES_PREFIX)
                    .build();
            
            ListObjectsV2Response listResponse = getS3Client().listObjectsV2(listRequest);
            
            Set<String> languages = listResponse.contents().stream()
                    .filter(s3Object -> s3Object.key().endsWith(".json"))
                    .map(s3Object -> {
                        String fileName = s3Object.key().substring(S3_LANGUAGES_PREFIX.length());
                        return fileName.substring(0, fileName.length() - 5);
                    })
                    .filter(langCode -> langCode.matches("^[a-z]{2}(-[a-z]{2})?$"))
                    .collect(Collectors.toSet());
            
            return languages.isEmpty() ? new HashSet<>() : languages;
        } catch (Exception e) {
            throw new BusinessException("Failed to list language files: " + e.getMessage(), e);
        }
    }
    

    @SuppressWarnings("unchecked")
    public Map<String, Object> getLanguageTranslations(String langCode) {
        return getLanguageTranslations(langCode, false);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getLanguageTranslations(String langCode, boolean skipCache) {
        langCode = langCode.toLowerCase();
        if (!langCode.matches("^[a-z]{2}(-[a-z]{2})?$")) {
            throw new BusinessException("Invalid language code format: " + langCode);
        }

        if (!skipCache && languageCache.containsKey(langCode)) {
            log.debug("Returning cached language translations for: {}", langCode);
            return languageCache.get(langCode);
        }
        
        if (skipCache) {
            log.debug("Cache bypassed for language: {}", langCode);
        }

        String s3Key = S3_LANGUAGES_PREFIX + langCode + ".json";
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            try (InputStream inputStream = getS3Client().getObject(getObjectRequest)) {
                Map<String, Object> translations = objectMapper.readValue(inputStream, Map.class);
                // Only cache if not skipping cache
                if (!skipCache) {
                    languageCache.put(langCode, translations);
                    log.debug("Cached language translations for: {}", langCode);
                }
                return translations;
            }
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFoundException("Language file not found: " + langCode);
        } catch (Exception e) {
            throw new BusinessException("Failed to load language file: " + e.getMessage(), e);
        }
    }

    public Map<String, Map<String, Object>> getAllLanguageTranslations() {
        return getAllLanguageTranslations(false);
    }

    public Map<String, Map<String, Object>> getAllLanguageTranslations(boolean skipCache) {
        Map<String, Map<String, Object>> allTranslations = new HashMap<>();
        for (String langCode : getAvailableLanguages()) {
            try {
                allTranslations.put(langCode, getLanguageTranslations(langCode, skipCache));
            } catch (ResourceNotFoundException e) {
                // Skip missing languages
            }
        }
        return allTranslations;
    }
    
    public void uploadTranslations(String langCode, Map<String, Object> translations) {
        langCode = langCode.toLowerCase();
        if (!langCode.matches("^[a-z]{2}(-[a-z]{2})?$")) {
            throw new BusinessException("Invalid language code format: " + langCode);
        }
        if (translations == null || translations.isEmpty()) {
            throw new BusinessException("Translations cannot be empty");
        }
        
        try {
            String s3Key = S3_LANGUAGES_PREFIX + langCode + ".json";
            String jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(translations);
            byte[] jsonBytes = jsonContent.getBytes(StandardCharsets.UTF_8);
            
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType("application/json")
                    .build();
            
            getS3Client().putObject(putRequest, RequestBody.fromBytes(jsonBytes));
            languageCache.remove(langCode);
        } catch (Exception e) {
            throw new BusinessException("Failed to save language file: " + e.getMessage(), e);
        }
    }
    
    public void deleteLanguage(String langCode) {
        langCode = langCode.toLowerCase();
        if (!langCode.matches("^[a-z]{2}(-[a-z]{2})?$")) {
            throw new BusinessException("Invalid language code format: " + langCode);
        }
        
        try {
            String s3Key = S3_LANGUAGES_PREFIX + langCode + ".json";
            try {
                HeadObjectRequest headRequest = HeadObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .build();
                getS3Client().headObject(headRequest);
            } catch (NoSuchKeyException e) {
                throw new ResourceNotFoundException("Language file not found: " + langCode);
            }
            
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            
            getS3Client().deleteObject(deleteRequest);
            languageCache.remove(langCode);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Failed to delete language file: " + e.getMessage(), e);
        }
    }
    
    public void clearCache() {
        languageCache.clear();
    }
    
    public void clearCache(String langCode) {
        languageCache.remove(langCode.toLowerCase());
    }

    /**
     * Get apple language translations for authenticated users (post-signin)
     * Uses separate S3 prefix: jsons/apple-languages/
     * Only returns apple language if userId matches configured appleUserId, otherwise returns normal language
     *
     * @param langCode Language code (e.g., "en", "ru", "zh")
     * @param userId User ID to check if they should get apple language
     * @return Map of translations (apple language if userId matches, normal language otherwise)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getAppleLanguageTranslations(String langCode, Long userId) {
        return getAppleLanguageTranslations(langCode, userId, false);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getAppleLanguageTranslations(String langCode, Long userId, boolean skipCache) {
        // If userId matches configured appleUserId, return apple language
        // Otherwise, return normal language
        if (appleUserId != null && userId != null && userId.equals(appleUserId)) {
            log.info("Fetching apple languages code: {} for user: {} (skipCache: {})", langCode, userId, skipCache);
            return getAppleLanguageTranslationsInternal(langCode, skipCache);
        } else {
            log.info("Fetching languages code: {} for user: {} (skipCache: {})", langCode, userId, skipCache);
            return getLanguageTranslations(langCode, skipCache);
        }
    }

    /**
     * Internal method to get apple language translations from S3
     *
     * @param langCode Language code (e.g., "en", "ru", "zh")
     * @return Map of translations
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getAppleLanguageTranslationsInternal(String langCode) {
        return getAppleLanguageTranslationsInternal(langCode, false);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getAppleLanguageTranslationsInternal(String langCode, boolean skipCache) {
        langCode = langCode.toLowerCase();
        if (!langCode.matches("^[a-z]{2}(-[a-z]{2})?$")) {
            throw new BusinessException("Invalid language code format: " + langCode);
        }

        if (!skipCache && appleLanguageCache.containsKey(langCode)) {
            log.debug("Returning cached apple language translations for: {}", langCode);
            return appleLanguageCache.get(langCode);
        }
        
        if (skipCache) {
            log.debug("Cache bypassed for apple language: {}", langCode);
        }

        String s3Key = S3_APPLE_LANGUAGES_PREFIX + langCode + ".json";
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            try (InputStream inputStream = getS3Client().getObject(getObjectRequest)) {
                Map<String, Object> translations = objectMapper.readValue(inputStream, Map.class);
                // Only cache if not skipping cache
                if (!skipCache) {
                    appleLanguageCache.put(langCode, translations);
                    log.debug("Cached apple language translations for: {}", langCode);
                }
                return translations;
            }
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFoundException("Apple language file not found: " + langCode);
        } catch (Exception e) {
            throw new BusinessException("Failed to load apple language file: " + e.getMessage(), e);
        }
    }

    /**
     * Get all apple language translations (all languages)
     * Returns translations for all available apple languages if userId matches, otherwise normal languages
     *
     * @param userId User ID to check if they should get apple language
     * @return Map of language code to translations
     */
    public Map<String, Map<String, Object>> getAllAppleLanguageTranslations(Long userId) {
        return getAllAppleLanguageTranslations(userId, false);
    }

    public Map<String, Map<String, Object>> getAllAppleLanguageTranslations(Long userId, boolean skipCache) {
        // If userId matches configured appleUserId, return apple languages
        // Otherwise, return normal languages
        if (appleUserId != null && userId != null && userId.equals(appleUserId)) {
            return getAllAppleLanguageTranslationsInternal(skipCache);
        } else {
            return getAllLanguageTranslations(skipCache);
        }
    }

    /**
     * Internal method to get all apple language translations from S3
     *
     * @param skipCache Whether to bypass cache
     * @return Map of language code to translations
     */
    private Map<String, Map<String, Object>> getAllAppleLanguageTranslationsInternal(boolean skipCache) {
        Map<String, Map<String, Object>> allTranslations = new HashMap<>();
        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(S3_APPLE_LANGUAGES_PREFIX)
                    .build();
            
            ListObjectsV2Response listResponse = getS3Client().listObjectsV2(listRequest);
            
            Set<String> languages = listResponse.contents().stream()
                    .filter(s3Object -> s3Object.key().endsWith(".json"))
                    .map(s3Object -> {
                        String fileName = s3Object.key().substring(S3_APPLE_LANGUAGES_PREFIX.length());
                        return fileName.substring(0, fileName.length() - 5);
                    })
                    .filter(langCode -> langCode.matches("^[a-z]{2}(-[a-z]{2})?$"))
                    .collect(Collectors.toSet());
            
            for (String langCode : languages) {
                try {
                    allTranslations.put(langCode, getAppleLanguageTranslationsInternal(langCode, skipCache));
                } catch (ResourceNotFoundException e) {
                    // Skip missing languages
                }
            }
        } catch (Exception e) {
            log.error("Failed to list apple language files: {}", e.getMessage(), e);
            throw new BusinessException("Failed to list apple language files: " + e.getMessage(), e);
        }
        return allTranslations;
    }

    /**
     * Clear cache for apple languages
     */
    public void clearAppleLanguageCache() {
        appleLanguageCache.clear();
    }

    /**
     * Clear cache for specific apple language
     */
    public void clearAppleLanguageCache(String langCode) {
        appleLanguageCache.remove(langCode.toLowerCase());
    }
}

