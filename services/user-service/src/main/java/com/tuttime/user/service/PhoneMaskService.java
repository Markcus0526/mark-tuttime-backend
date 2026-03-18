package com.tuttime.user.service;

import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
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

/**
 * Phone mask service for loading and serving phone mask files from S3
 */
@Slf4j
@Service
public class PhoneMaskService {

    private static final String S3_PHONE_MASKS_KEY = "jsons/phone-masks.json";
    
    private final ObjectMapper objectMapper;
    private List<Map<String, Object>> phoneMasksCache;
    
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

    public PhoneMaskService(ObjectMapper objectMapper) {
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
     * Get all phone masks
     *
     * @param skipCache Whether to bypass cache
     * @return List of phone mask objects
     */
    public List<Map<String, Object>> getPhoneMasks(boolean skipCache) {
        if (!skipCache && phoneMasksCache != null) {
            log.debug("Returning cached phone masks");
            return phoneMasksCache;
        }
        
        if (skipCache) {
            log.debug("Cache bypassed for phone masks");
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(S3_PHONE_MASKS_KEY)
                    .build();

            try (InputStream inputStream = getS3Client().getObject(getObjectRequest)) {
                List<Map<String, Object>> phoneMasks = objectMapper.readValue(
                        inputStream, 
                        new TypeReference<List<Map<String, Object>>>() {});
                
                // Only cache if not skipping cache
                if (!skipCache) {
                    phoneMasksCache = phoneMasks;
                    log.debug("Cached phone masks");
                }
                return phoneMasks;
            }
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFoundException("Phone masks file not found");
        } catch (Exception e) {
            throw new BusinessException("Failed to load phone masks file: " + e.getMessage(), e);
        }
    }

    /**
     * Get all phone masks (with default cache behavior)
     *
     * @return List of phone mask objects
     */
    public List<Map<String, Object>> getPhoneMasks() {
        return getPhoneMasks(false);
    }

    /**
     * Get phone mask by country code (ISO code)
     *
     * @param isoCode Country ISO code (e.g., "US", "RU", "GB")
     * @param skipCache Whether to bypass cache
     * @return Phone mask object for the country, or null if not found
     */
    public Map<String, Object> getPhoneMaskByIso(String isoCode, boolean skipCache) {
        if (isoCode == null || isoCode.isEmpty()) {
            throw new BusinessException("ISO code cannot be empty");
        }
        
        List<Map<String, Object>> phoneMasks = getPhoneMasks(skipCache);
        
        return phoneMasks.stream()
                .filter(mask -> isoCode.equalsIgnoreCase((String) mask.get("iso")))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get phone mask by country code (with default cache behavior)
     *
     * @param isoCode Country ISO code (e.g., "US", "RU", "GB")
     * @return Phone mask object for the country, or null if not found
     */
    public Map<String, Object> getPhoneMaskByIso(String isoCode) {
        return getPhoneMaskByIso(isoCode, false);
    }

    /**
     * Get phone mask by phone code (country calling code)
     *
     * @param phoneCode Country calling code (e.g., "+1", "+7", "+44")
     * @param skipCache Whether to bypass cache
     * @return Phone mask object for the country, or null if not found
     */
    public Map<String, Object> getPhoneMaskByCode(String phoneCode, boolean skipCache) {
        if (phoneCode == null || phoneCode.isEmpty()) {
            throw new BusinessException("Phone code cannot be empty");
        }
        
        // Normalize phone code (ensure it starts with +)
        String normalizedCode = phoneCode.startsWith("+") ? phoneCode : "+" + phoneCode;
        
        List<Map<String, Object>> phoneMasks = getPhoneMasks(skipCache);
        
        return phoneMasks.stream()
                .filter(mask -> normalizedCode.equals(mask.get("code")))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get phone mask by phone code (with default cache behavior)
     *
     * @param phoneCode Country calling code (e.g., "+1", "+7", "+44")
     * @return Phone mask object for the country, or null if not found
     */
    public Map<String, Object> getPhoneMaskByCode(String phoneCode) {
        return getPhoneMaskByCode(phoneCode, false);
    }

    /**
     * Upload or update phone masks
     *
     * @param phoneMasks List of phone mask objects
     */
    public void uploadPhoneMasks(List<Map<String, Object>> phoneMasks) {
        if (phoneMasks == null || phoneMasks.isEmpty()) {
            throw new BusinessException("Phone masks cannot be empty");
        }
        
        try {
            String jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(phoneMasks);
            byte[] jsonBytes = jsonContent.getBytes(StandardCharsets.UTF_8);
            
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(S3_PHONE_MASKS_KEY)
                    .contentType("application/json")
                    .build();
            
            getS3Client().putObject(putRequest, RequestBody.fromBytes(jsonBytes));
            phoneMasksCache = null; // Clear cache
            log.info("Phone masks uploaded successfully");
        } catch (Exception e) {
            throw new BusinessException("Failed to save phone masks file: " + e.getMessage(), e);
        }
    }

    /**
     * Clear cache
     */
    public void clearCache() {
        phoneMasksCache = null;
    }
}

