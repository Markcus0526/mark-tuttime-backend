package com.tuttime.admin.service.impl;

import com.tuttime.admin.service.VideoStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * AWS S3-Compatible Video Storage Service Implementation
 *
 * <p>Stores videos in S3-compatible storage (Selectel S3).
 * Videos are stored as-is without processing (unlike images).
 */
@Slf4j
@Service
public class S3VideoStorageServiceImpl implements VideoStorageService {

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

	@Value("${app.s3.base-url:}")
	private String baseUrl; // Optional: CDN or public URL base

	@Value("${app.s3.presigned-url-expiration-hours:168}")  // Default: 7 days (168 hours)
	private long presignedUrlExpirationHours;

	@Value("${app.video.max-size:524288000}") // 500MB default
	private long maxVideoSize;

	@Value("${app.thumbnail.max-size:5242880}") // 5MB default
	private long maxThumbnailSize;

	private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
			"video/mp4",
			"video/webm",
			"video/quicktime", // MOV
			"video/x-msvideo", // AVI
			"video/mpeg"
	);

	private static final List<String> ALLOWED_THUMBNAIL_TYPES = Arrays.asList(
			"image/jpeg",
			"image/jpg",
			"image/png",
			"image/gif",
			"image/webp"
	);

	private S3Client s3Client;
	private S3Presigner s3Presigner;

	/**
	 * Initializes S3 client with custom endpoint for S3-compatible storage.
	 */
	private S3Client getS3Client() {
		if (s3Client == null) {
			// Validate S3 credentials are configured
			if (accessKey == null || accessKey.trim().isEmpty() ||
					secretKey == null || secretKey.trim().isEmpty()) {
				throw new RuntimeException("S3 credentials are not configured. " +
						"Please set APP_S3_ACCESS_KEY and APP_S3_SECRET_KEY environment variables.");
			}

			s3Client = S3Client.builder()
					.endpointOverride(URI.create(s3Endpoint))
					.region(Region.of(s3Region))
					.credentialsProvider(StaticCredentialsProvider.create(
							AwsBasicCredentials.create(accessKey, secretKey)))
					.forcePathStyle(true) // Required for S3-compatible storage
					.build();
			log.info("S3 client initialized for video storage with endpoint: {}, region: {}, bucket: {}",
					s3Endpoint, s3Region, bucketName);
		}
		return s3Client;
	}

	/**
	 * Gets the S3 presigner (lazy initialization)
	 */
	private S3Presigner getS3Presigner() {
		if (s3Presigner == null && (accessKey != null && !accessKey.trim().isEmpty())) {
			try {
				s3Presigner = S3Presigner.builder()
						.endpointOverride(URI.create(s3Endpoint))
						.region(Region.of(s3Region))
						.credentialsProvider(StaticCredentialsProvider.create(
								AwsBasicCredentials.create(accessKey, secretKey)))
						.build();
				log.debug("S3 presigner initialized for video storage");
			} catch (Exception e) {
				log.warn("Failed to initialize S3 presigner: {}", e.getMessage());
			}
		}
		return s3Presigner;
	}

	@Override
	public void validateVideo(MultipartFile file) throws IllegalArgumentException {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("Video file is required");
		}

		// Validate file size
		if (file.getSize() > maxVideoSize) {
			throw new IllegalArgumentException(
					String.format("Video file size (%d bytes) exceeds maximum allowed size (%d bytes)",
							file.getSize(), maxVideoSize));
		}

		// Validate content type
		String contentType = file.getContentType();
		if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
			// Try to infer from filename if content type is not set
			String filename = file.getOriginalFilename();
			if (filename != null) {
				String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
				if (!Arrays.asList("mp4", "webm", "mov", "avi", "mpeg").contains(extension)) {
					throw new IllegalArgumentException(
							String.format("Invalid video file format. Allowed formats: MP4, WebM, MOV, AVI, MPEG. " +
									"Received: %s", contentType != null ? contentType : "unknown"));
				}
			} else {
				throw new IllegalArgumentException(
						String.format("Invalid video file format. Allowed formats: MP4, WebM, MOV, AVI, MPEG. " +
								"Received: %s", contentType != null ? contentType : "unknown"));
			}
		}
	}

	@Override
	public String uploadVideo(MultipartFile file) throws IOException {
		validateVideo(file);

		// Generate unique video key: videos/admin/{uuid}.{extension}
		String originalFilename = file.getOriginalFilename();
		String extension = "mp4"; // Default extension
		if (originalFilename != null && originalFilename.contains(".")) {
			extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
		} else {
			// Infer extension from content type
			String contentType = file.getContentType();
			if (contentType != null) {
				if (contentType.contains("webm")) extension = "webm";
				else if (contentType.contains("quicktime") || contentType.contains("mov")) extension = "mov";
				else if (contentType.contains("x-msvideo") || contentType.contains("avi")) extension = "avi";
				else if (contentType.contains("mpeg")) extension = "mpeg";
			}
		}

		String videoKey = String.format("videos/admin/%s.%s", UUID.randomUUID().toString(), extension);

		// Upload to S3
		try {
			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
					.bucket(bucketName)
					.key(videoKey)
					.contentType(file.getContentType() != null ? file.getContentType() : "video/mp4")
					.contentLength(file.getSize())
					.build();

			try (InputStream inputStream = file.getInputStream()) {
				getS3Client().putObject(putObjectRequest,
						RequestBody.fromInputStream(inputStream, file.getSize()));
			}

			log.info("Video uploaded to S3: key={}, size={} bytes, contentType={}",
					videoKey, file.getSize(), file.getContentType());

			return videoKey;
		} catch (S3Exception e) {
			log.error("Failed to upload video to S3: {}", e.getMessage(), e);
			throw new IOException("Failed to upload video to S3: " + e.getMessage(), e);
		}
	}

	@Override
	public String getVideoUrl(String videoKey) {
		if (videoKey == null || videoKey.trim().isEmpty()) {
			return null;
		}

		// Normalize videoKey: remove leading slashes
		videoKey = videoKey.trim();
		while (videoKey.startsWith("/")) {
			videoKey = videoKey.substring(1);
		}

		// If it's already a full URL (not an S3 key), return as-is
		if (videoKey.startsWith("http://") || videoKey.startsWith("https://")) {
			return videoKey;
		}

		// If base URL is configured, use it (CDN or public URL) - same logic as user-service
		if (baseUrl != null && !baseUrl.trim().isEmpty()) {
			String base = baseUrl.trim();
			String url = base.endsWith("/")
					? base + videoKey
					: base + "/" + videoKey;
			log.debug("Generated video URL from base URL: baseUrl={}, key={}, url={}", baseUrl, videoKey, url);
			return url;
		}

		// Otherwise, generate presigned URL (same logic as user-service)
		try {
			S3Presigner presigner = getS3Presigner();
			if (presigner == null) {
				log.error("S3 presigner is not available (credentials not configured). Cannot generate video URL for key: {}", videoKey);
				return null;
			}

			GetObjectRequest getObjectRequest = GetObjectRequest.builder()
					.bucket(bucketName)
					.key(videoKey)
					.build();

			PresignedGetObjectRequest presignedRequest = presigner
					.presignGetObject(GetObjectPresignRequest.builder()
							.signatureDuration(Duration.ofHours(presignedUrlExpirationHours))
							.getObjectRequest(getObjectRequest)
							.build());

			String url = presignedRequest.url().toString();
			log.debug("Generated presigned video URL: {}", url);
			return url;
		} catch (Exception e) {
			log.error("Failed to generate presigned URL for video key {}: {}", videoKey, e.getMessage(), e);
			// Return null to indicate error (same pattern as user-service fallback)
			return null;
		}
	}

	@Override
	public void validateThumbnail(MultipartFile file) throws IllegalArgumentException {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("Thumbnail file is required");
		}

		// Validate file size
		if (file.getSize() > maxThumbnailSize) {
			throw new IllegalArgumentException(
					String.format("Thumbnail file size (%d bytes) exceeds maximum allowed size (%d bytes)",
							file.getSize(), maxThumbnailSize));
		}

		// Validate content type
		String contentType = file.getContentType();
		if (contentType == null || !ALLOWED_THUMBNAIL_TYPES.contains(contentType.toLowerCase())) {
			// Try to infer from filename if content type is not set
			String filename = file.getOriginalFilename();
			if (filename != null) {
				String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
				if (!Arrays.asList("jpg", "jpeg", "png", "gif", "webp").contains(extension)) {
					throw new IllegalArgumentException(
							String.format("Invalid thumbnail file format. Allowed formats: JPEG, PNG, GIF, WEBP. " +
									"Received: %s", contentType != null ? contentType : "unknown"));
				}
			} else {
				throw new IllegalArgumentException(
						String.format("Invalid thumbnail file format. Allowed formats: JPEG, PNG, GIF, WEBP. " +
								"Received: %s", contentType != null ? contentType : "unknown"));
			}
		}
	}

	@Override
	public String uploadThumbnail(MultipartFile file) throws IOException {
		validateThumbnail(file);

		// Generate unique thumbnail key: thumbnails/admin/{uuid}.{extension}
		String originalFilename = file.getOriginalFilename();
		String extension = "jpg"; // Default extension
		if (originalFilename != null && originalFilename.contains(".")) {
			extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
			// Normalize jpeg to jpg
			if (extension.equals("jpeg")) {
				extension = "jpg";
			}
		} else {
			// Infer extension from content type
			String contentType = file.getContentType();
			if (contentType != null) {
				if (contentType.contains("jpeg") || contentType.contains("jpg")) extension = "jpg";
				else if (contentType.contains("png")) extension = "png";
				else if (contentType.contains("gif")) extension = "gif";
				else if (contentType.contains("webp")) extension = "webp";
			}
		}

		String thumbnailKey = String.format("thumbnails/admin/%s.%s", UUID.randomUUID().toString(), extension);

		// Upload to S3
		try {
			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
					.bucket(bucketName)
					.key(thumbnailKey)
					.contentType(file.getContentType() != null ? file.getContentType() : "image/jpeg")
					.contentLength(file.getSize())
					.build();

			try (InputStream inputStream = file.getInputStream()) {
				getS3Client().putObject(putObjectRequest,
						RequestBody.fromInputStream(inputStream, file.getSize()));
			}

			log.info("Thumbnail uploaded to S3: key={}, size={} bytes, contentType={}",
					thumbnailKey, file.getSize(), file.getContentType());

			return thumbnailKey;
		} catch (S3Exception e) {
			log.error("Failed to upload thumbnail to S3: {}", e.getMessage(), e);
			throw new IOException("Failed to upload thumbnail to S3: " + e.getMessage(), e);
		}
	}

	@Override
	public String getThumbnailUrl(String thumbnailKey) {
		if (thumbnailKey == null || thumbnailKey.trim().isEmpty()) {
			return null;
		}

		// Normalize thumbnailKey: remove leading slashes
		thumbnailKey = thumbnailKey.trim();
		while (thumbnailKey.startsWith("/")) {
			thumbnailKey = thumbnailKey.substring(1);
		}

		// If it's already a full URL (not an S3 key), return as-is
		if (thumbnailKey.startsWith("http://") || thumbnailKey.startsWith("https://")) {
			return thumbnailKey;
		}

		// If base URL is configured, use it (CDN or public URL)
		if (baseUrl != null && !baseUrl.trim().isEmpty()) {
			String base = baseUrl.trim();
			String url = base.endsWith("/")
					? base + thumbnailKey
					: base + "/" + thumbnailKey;
			log.debug("Generated thumbnail URL from base URL: baseUrl={}, key={}, url={}", baseUrl, thumbnailKey, url);
			return url;
		}

		// Otherwise, generate presigned URL
		try {
			S3Presigner presigner = getS3Presigner();
			if (presigner == null) {
				log.error("S3 presigner is not available (credentials not configured). Cannot generate thumbnail URL for key: {}", thumbnailKey);
				return null;
			}

			GetObjectRequest getObjectRequest = GetObjectRequest.builder()
					.bucket(bucketName)
					.key(thumbnailKey)
					.build();

			PresignedGetObjectRequest presignedRequest = presigner
					.presignGetObject(GetObjectPresignRequest.builder()
							.signatureDuration(Duration.ofHours(presignedUrlExpirationHours))
							.getObjectRequest(getObjectRequest)
							.build());

			String url = presignedRequest.url().toString();
			log.debug("Generated presigned thumbnail URL: {}", url);
			return url;
		} catch (Exception e) {
			log.error("Failed to generate presigned URL for thumbnail key {}: {}", thumbnailKey, e.getMessage(), e);
			return null;
		}
	}

	@Override
	public boolean deleteVideo(String videoKey) {
		if (videoKey == null || videoKey.trim().isEmpty()) {
			return false;
		}

		// Normalize videoKey: remove leading slashes
		videoKey = videoKey.trim();
		while (videoKey.startsWith("/")) {
			videoKey = videoKey.substring(1);
		}

		// If it's already a full URL, extract the key from it
		if (videoKey.startsWith("http://") || videoKey.startsWith("https://")) {
			// Try to extract S3 key from URL
			// For presigned URLs or CDN URLs, we need to extract the key
			// This is a simple extraction - may need adjustment based on URL format
			try {
				java.net.URI uri = new java.net.URI(videoKey);
				String path = uri.getPath();
				if (path != null && path.startsWith("/")) {
					path = path.substring(1);
				}
				// Check if path looks like an S3 key (contains "videos/")
				if (path != null && (path.contains("videos/") || path.startsWith("videos/"))) {
					videoKey = path;
				} else {
					log.warn("Cannot extract video key from URL: {}", videoKey);
					return false;
				}
			} catch (Exception e) {
				log.warn("Failed to parse video URL to extract key: {}", videoKey, e);
				return false;
			}
		}

		try {
			DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
					.bucket(bucketName)
					.key(videoKey)
					.build();

			getS3Client().deleteObject(deleteRequest);
			log.info("Deleted video from S3: key={}", videoKey);
			return true;
		} catch (S3Exception e) {
			if (e.statusCode() == 404) {
				log.warn("Video not found in S3 (may have been already deleted): key={}", videoKey);
				return false;
			}
			log.error("Failed to delete video from S3: key={}, error={}", videoKey, e.getMessage(), e);
			return false;
		} catch (Exception e) {
			log.error("Unexpected error deleting video from S3: key={}", videoKey, e);
			return false;
		}
	}

	@Override
	public boolean deleteThumbnail(String thumbnailKey) {
		if (thumbnailKey == null || thumbnailKey.trim().isEmpty()) {
			return false;
		}

		// Normalize thumbnailKey: remove leading slashes
		thumbnailKey = thumbnailKey.trim();
		while (thumbnailKey.startsWith("/")) {
			thumbnailKey = thumbnailKey.substring(1);
		}

		// If it's already a full URL, extract the key from it
		if (thumbnailKey.startsWith("http://") || thumbnailKey.startsWith("https://")) {
			// Try to extract S3 key from URL
			try {
				java.net.URI uri = new java.net.URI(thumbnailKey);
				String path = uri.getPath();
				if (path != null && path.startsWith("/")) {
					path = path.substring(1);
				}
				// Check if path looks like an S3 key (contains "thumbnails/")
				if (path != null && (path.contains("thumbnails/") || path.startsWith("thumbnails/"))) {
					thumbnailKey = path;
				} else {
					log.warn("Cannot extract thumbnail key from URL: {}", thumbnailKey);
					return false;
				}
			} catch (Exception e) {
				log.warn("Failed to parse thumbnail URL to extract key: {}", thumbnailKey, e);
				return false;
			}
		}

		try {
			DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
					.bucket(bucketName)
					.key(thumbnailKey)
					.build();

			getS3Client().deleteObject(deleteRequest);
			log.info("Deleted thumbnail from S3: key={}", thumbnailKey);
			return true;
		} catch (S3Exception e) {
			if (e.statusCode() == 404) {
				log.warn("Thumbnail not found in S3 (may have been already deleted): key={}", thumbnailKey);
				return false;
			}
			log.error("Failed to delete thumbnail from S3: key={}, error={}", thumbnailKey, e.getMessage(), e);
			return false;
		} catch (Exception e) {
			log.error("Unexpected error deleting thumbnail from S3: key={}", thumbnailKey, e);
			return false;
		}
	}
}

