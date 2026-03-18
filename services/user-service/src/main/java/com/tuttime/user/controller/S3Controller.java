package com.tuttime.user.controller;

import com.tuttime.common.dto.ApiResponse;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.user.dto.PresignedUrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.time.Duration;

/**
 * REST Controller for S3 File Operations
 *
 * <p>This controller handles HTTP requests related to S3 file access,
 * providing endpoints to generate pre-signed URLs for S3 objects.
 *
 * <p><b>Endpoints:</b>
 * <ul>
 *   <li>GET /api/v1/users/s3/presigned-url - Generate pre-signed URL for an S3 file</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users/s3")
@RequiredArgsConstructor
@Tag(name = "S3 File Management", description = "S3 file access APIs")
public class S3Controller {

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

    @Value("${app.image.s3.presigned-url-expiration-hours:24}")
    private long presignedUrlExpirationHours;

    private S3Client s3Client;
    private S3Presigner s3Presigner;

    /**
     * Initializes S3 client with custom endpoint for S3-compatible storage.
     */
    private S3Client getS3Client() {
        if (s3Client == null) {
            s3Client = S3Client.builder()
                    .endpointOverride(URI.create(s3Endpoint))
                    .region(Region.of(s3Region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .forcePathStyle(true) // Required for S3-compatible storage
                    .build();
            log.info("S3 client initialized with endpoint: {}, region: {}, bucket: {}", 
                    s3Endpoint, s3Region, bucketName);
        }
        return s3Client;
    }

    /**
     * Gets S3 presigner for generating presigned URLs.
     */
    private S3Presigner getS3Presigner() {
        if (s3Presigner == null) {
            s3Presigner = S3Presigner.builder()
                    .endpointOverride(URI.create(s3Endpoint))
                    .region(Region.of(s3Region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .build();
        }
        return s3Presigner;
    }

    /**
     * Generate pre-signed URL for an S3 file
     *
     * @param fileKey S3 file key/path (e.g., "jsons/cities/CC.json", "users/123/avatar.jpg")
     * @return ResponseEntity containing pre-signed URL
     */
    @GetMapping("/presigned-url")
    @Operation(summary = "Generate pre-signed URL for S3 file", 
            description = "Generates a pre-signed URL for accessing an S3 file. " +
                    "The URL expiration time is configured via environment variable " +
                    "(app.image.s3.presigned-url-expiration-hours, default: 24 hours). " +
                    "Example file keys: 'jsons/cities/CC.json', 'users/123/avatar.jpg'")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> generatePresignedUrl(
            @Parameter(description = "S3 file key/path (e.g., jsons/cities/CC.json)", required = true)
            @RequestParam String fileKey) {
        
        // Validate fileKey parameter
        if (fileKey == null || fileKey.trim().isEmpty()) {
            log.warn("Invalid fileKey parameter: empty or null");
            throw new com.tuttime.common.exception.ValidationException("File key is required");
        }
        
        log.debug("Generating pre-signed URL for file: {}", fileKey);
        
        // Validate file exists in S3
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();
            
            getS3Client().headObject(headRequest);
        } catch (NoSuchKeyException e) {
            log.warn("File not found in S3: {}", fileKey);
            throw new ResourceNotFoundException("File not found: " + fileKey);
        } catch (S3Exception e) {
            log.error("Failed to check file existence in S3: key={}, error={}", fileKey, e.getMessage(), e);
            throw new RuntimeException("Failed to access S3 file: " + e.getMessage(), e);
        }
        
        // Generate presigned URL
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            PresignedGetObjectRequest presignedRequest = getS3Presigner()
                    .presignGetObject(GetObjectPresignRequest.builder()
                            .signatureDuration(Duration.ofHours(presignedUrlExpirationHours))
                            .getObjectRequest(getObjectRequest)
                            .build());

            String presignedUrl = presignedRequest.url().toString();
            
            PresignedUrlResponse response = PresignedUrlResponse.builder()
                    .fileKey(fileKey)
                    .presignedUrl(presignedUrl)
                    .expirationHours(presignedUrlExpirationHours)
                    .build();
            
            log.info("Generated pre-signed URL for file: {} (expires in {} hours)", fileKey, presignedUrlExpirationHours);
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Pre-signed URL generated successfully", response));
                    
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for file: {}", fileKey, e);
            throw new RuntimeException("Failed to generate pre-signed URL: " + e.getMessage(), e);
        }
    }
}
