package com.tuttime.admin.controller;

import com.tuttime.admin.dto.PresignedUrlResponse;
import com.tuttime.common.dto.ApiResponse;
import com.tuttime.common.exception.ValidationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

/**
 * REST Controller for S3 File Operations (Admin)
 *
 * <p>This controller handles HTTP requests related to S3 file uploads for admin operations,
 * providing endpoints to generate pre-signed URLs for video uploads.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/s3")
@RequiredArgsConstructor
@Tag(name = "Admin S3 File Management", description = "S3 file upload APIs for admin")
public class S3Controller {

	@Value("${app.s3.endpoint:https://s3.ru-7.storage.selcloud.ru}")
	private String s3Endpoint;

	@Value("${app.s3.region:ru-7}")
	private String s3Region;

	@Value("${app.s3.bucket:tuttime-storage}")
	private String bucketName;

	@Value("${app.s3.access-key:}")
	private String accessKey;

	@Value("${app.s3.secret-key:}")
	private String secretKey;

	@Value("${app.s3.presigned-url-expiration-hours:1}")
	private long presignedUrlExpirationHours;

	private S3Presigner s3Presigner;


	/**
	 * Gets S3 presigner for generating presigned URLs.
	 */
	private S3Presigner getS3Presigner() {
		// Validate S3 credentials are configured
		if (accessKey == null || accessKey.trim().isEmpty() ||
				secretKey == null || secretKey.trim().isEmpty()) {
			throw new RuntimeException("S3 credentials are not configured. " +
					"Please set APP_S3_ACCESS_KEY and APP_S3_SECRET_KEY environment variables.");
		}

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
	 * Generate pre-signed URL for video upload
	 *
	 * @param fileName    Optional file name. If not provided, a UUID-based name will be generated.
	 * @param contentType Content type of the video file (e.g., "video/mp4")
	 * @return ResponseEntity containing pre-signed URL for upload
	 */
	@GetMapping("/presigned-url/upload")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
	@Operation(summary = "Generate pre-signed URL for video upload",
			description = "Generates a pre-signed URL for uploading a video file to S3. " +
					"The URL expiration time is configured via environment variable " +
					"(app.s3.presigned-url-expiration-hours, default: 1 hour).")
	public ResponseEntity<ApiResponse<PresignedUrlResponse>> generateUploadPresignedUrl(
			@Parameter(description = "File name (optional, will generate UUID-based name if not provided)")
			@RequestParam(required = false) String fileName,
			@Parameter(description = "Content type (e.g., video/mp4)", required = true)
			@RequestParam String contentType) {

		// Validate contentType parameter
		if (contentType == null || contentType.trim().isEmpty()) {
			log.warn("Invalid contentType parameter: empty or null");
			throw new ValidationException("Content type is required");
		}

		// Generate file key
		String fileKey;
		if (fileName != null && !fileName.trim().isEmpty()) {
			// Sanitize fileName to prevent path traversal
			String sanitizedFileName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
			fileKey = "videos/admin/" + sanitizedFileName;
		} else {
			// Generate UUID-based file name with extension based on content type
			String extension = getExtensionFromContentType(contentType);
			fileKey = "videos/admin/" + UUID.randomUUID().toString() + extension;
		}

		log.debug("Generating pre-signed URL for video upload: fileKey={}, contentType={}", fileKey, contentType);

		// Generate presigned URL for PUT operation
		try {
			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
					.bucket(bucketName)
					.key(fileKey)
					.contentType(contentType)
					.build();

			PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
					.signatureDuration(Duration.ofHours(presignedUrlExpirationHours))
					.putObjectRequest(putObjectRequest)
					.build();

			String presignedUrl = getS3Presigner()
					.presignPutObject(presignRequest)
					.url()
					.toString();

			PresignedUrlResponse response = PresignedUrlResponse.builder()
					.fileKey(fileKey)
					.presignedUrl(presignedUrl)
					.expirationHours(presignedUrlExpirationHours)
					.build();

			log.info("Generated pre-signed URL for video upload: fileKey={} (expires in {} hours)",
					fileKey, presignedUrlExpirationHours);

			return ResponseEntity.ok(ApiResponse.success(
					"Pre-signed URL generated successfully", response));

		} catch (RuntimeException e) {
			// Re-throw runtime exceptions (like missing credentials) as-is
			log.error("Failed to generate presigned URL for video upload: fileKey={}", fileKey, e);
			throw e;
		} catch (Exception e) {
			log.error("Failed to generate presigned URL for video upload: fileKey={}", fileKey, e);
			throw new RuntimeException("Failed to generate pre-signed URL: " + e.getMessage(), e);
		}
	}

	/**
	 * Gets file extension from content type.
	 */
	private String getExtensionFromContentType(String contentType) {
		if (contentType == null) {
			return ".mp4"; // Default extension
		}

		String lowerContentType = contentType.toLowerCase();
		if (lowerContentType.contains("mp4")) {
			return ".mp4";
		} else if (lowerContentType.contains("webm")) {
			return ".webm";
		} else if (lowerContentType.contains("quicktime") || lowerContentType.contains("mov")) {
			return ".mov";
		} else if (lowerContentType.contains("x-msvideo") || lowerContentType.contains("avi")) {
			return ".avi";
		} else if (lowerContentType.contains("mpeg")) {
			return ".mpeg";
		} else {
			return ".mp4"; // Default extension
		}
	}
}

