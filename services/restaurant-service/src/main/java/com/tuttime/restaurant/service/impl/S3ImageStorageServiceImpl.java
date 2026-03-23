package com.tuttime.restaurant.service.impl;

import com.tuttime.restaurant.dto.AboutImageResult;
import com.tuttime.restaurant.service.ImageStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * AWS S3-Compatible Image Storage Service Implementation
 * 
 * <p>Stores images in S3-compatible storage (Selectel S3).
 * All images are processed before storage:
 * <ul>
 *   <li>Resized to maximum 1024x1024 pixels (maintaining aspect ratio)</li>
 *   <li>Converted to JPEG format</li>
 *   <li>Compressed for optimal file size</li>
 * </ul>
 */
@Slf4j
@Service
public class S3ImageStorageServiceImpl implements ImageStorageService {

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

    @Value("${app.image.max-size:5242880}") // 5MB default
    private long maxImageSize;

    @Value("${app.image.max-dimension:1024}") // Max width/height in pixels
    private int maxDimension;

    @Value("${app.image.quality:0.85}") // JPEG quality (0.0 to 1.0)
    private float jpegQuality;

    @Value("${app.image.thumbnail.tiny:64}") // Tiny thumbnail size in pixels (64x64 - doubled for retina)
    private int tinyDimension;

    @Value("${app.image.thumbnail.small:200}") // Small thumbnail size in pixels (200x200 - increased from 128 for better quality)
    private int smallDimension;

    @Value("${app.image.thumbnail.medium:512}") // Medium thumbnail size in pixels (512x512)
    private int mediumDimension;

    @Value("${app.image.thumbnail.quality:0.90}") // Thumbnail JPEG quality (increased from 0.80 for better visual quality)
    private float thumbnailQuality;

    @Value("${app.image.s3.base-url:}")
    private String baseUrl; // Optional: CDN or public URL base

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
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

    @Override
    public String uploadImage(MultipartFile file, String userId) throws IOException {
        validateImage(file);

        // Process image: resize, convert to JPEG, compress
        byte[] processedImage = processImage(file);

        // Generate unique image key: users/{userId}/{uuid}.jpg
        String imageKey = String.format("users/%s/%s.jpg", userId, UUID.randomUUID().toString());

        // Upload to S3
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(imageKey)
                    .contentType("image/jpeg")
                    .contentLength((long) processedImage.length)
                    .build();

            getS3Client().putObject(putObjectRequest, 
                    RequestBody.fromInputStream(new ByteArrayInputStream(processedImage), processedImage.length));

            log.info("Image uploaded to S3 for user {}: key={}, size={} bytes", 
                    userId, imageKey, processedImage.length);

            return imageKey;
        } catch (S3Exception e) {
            log.error("Failed to upload image to S3: {}", e.getMessage(), e);
            throw new IOException("Failed to upload image to S3: " + e.getMessage(), e);
        }
    }

    /**
     * Uploads an image file and generates multiple thumbnail sizes for optimal performance.
     * 
     * <p>This method is optimized for various mobile and web use cases:
     * <ul>
     *   <li>Tiny (32x32px, ~1-3KB) - for map markers, compact lists, small avatars</li>
     *   <li>Small (128x128px, ~5-10KB) - for chat headers, profile cards, medium avatars</li>
     *   <li>Medium (512x512px, ~30-50KB) - for profile previews, modals, large cards</li>
     *   <li>Original (1024x1024px, ~100-300KB) - for full profile view, detailed display</li>
     * </ul>
     * 
     * @param file Image file to upload
     * @param userId User ID for organizing images
     * @return ProfileImageResult containing keys for all sizes
     * @throws IOException if file upload fails
     * @throws IllegalArgumentException if file is invalid
     */
    public AboutImageResult uploadImageWithThumbnail(MultipartFile file, String userId) throws IOException {
        validateImage(file);

        // Read original image once
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        if (originalImage == null) {
            throw new IOException("Unable to read image file");
        }

        // Generate unique base UUID for this upload
        String baseUuid = UUID.randomUUID().toString();
        String basePath = String.format("users/%s/%s", userId, baseUuid);

        try {
            // 1. Generate and upload tiny thumbnail (32x32px)
            BufferedImage tinyImage = resizeImageSquare(originalImage, tinyDimension);
            byte[] tinyBytes = convertToJpeg(tinyImage, thumbnailQuality);
            String tinyKey = basePath + "_tiny.jpg";
            uploadToS3(tinyKey, tinyBytes, "image/jpeg");
            log.info("Tiny thumbnail uploaded: key={}, size={} bytes ({}x{}px)", 
                    tinyKey, tinyBytes.length, tinyDimension, tinyDimension);

            // 2. Generate and upload small thumbnail (128x128px)
            BufferedImage smallImage = resizeImageSquare(originalImage, smallDimension);
            byte[] smallBytes = convertToJpeg(smallImage, thumbnailQuality);
            String smallKey = basePath + "_small.jpg";
            uploadToS3(smallKey, smallBytes, "image/jpeg");
            log.info("Small thumbnail uploaded: key={}, size={} bytes ({}x{}px)", 
                    smallKey, smallBytes.length, smallDimension, smallDimension);

            // 3. Generate and upload medium thumbnail (512x512px)
            BufferedImage mediumImage = resizeImageSquare(originalImage, mediumDimension);
            byte[] mediumBytes = convertToJpeg(mediumImage, thumbnailQuality);
            String mediumKey = basePath + "_medium.jpg";
            uploadToS3(mediumKey, mediumBytes, "image/jpeg");
            log.info("Medium thumbnail uploaded: key={}, size={} bytes ({}x{}px)", 
                    mediumKey, mediumBytes.length, mediumDimension, mediumDimension);

            // 4. Generate and upload original (1024x1024px max)
            BufferedImage processedOriginal = resizeImage(originalImage, maxDimension);
            byte[] originalBytes = convertToJpeg(processedOriginal, jpegQuality);
            String originalKey = basePath + ".jpg";
            uploadToS3(originalKey, originalBytes, "image/jpeg");
            log.info("Original uploaded: key={}, size={} bytes", originalKey, originalBytes.length);

            return AboutImageResult.builder()
                    .tinyKey(tinyKey)
                    .smallKey(smallKey)
                    .mediumKey(mediumKey)
                    .originalKey(originalKey)
                    .build();

        } catch (S3Exception e) {
            log.error("Failed to upload image with thumbnails to S3: {}", e.getMessage(), e);
            throw new IOException("Failed to upload image to S3: " + e.getMessage(), e);
        }
    }

    /**
     * Uploads bytes to S3.
     */
    private void uploadToS3(String key, byte[] data, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .contentLength((long) data.length)
                .build();

        getS3Client().putObject(putObjectRequest,
                RequestBody.fromInputStream(new ByteArrayInputStream(data), data.length));
    }

    /**
     * Resizes image to a square of specified dimension (center crop).
     * 
     * <p>Uses progressive downscaling with BICUBIC interpolation for maximum quality:
     * <ul>
     *   <li>Center crops to square first</li>
     *   <li>Progressively halves dimensions until close to target (reduces artifacts)</li>
     *   <li>Uses BICUBIC interpolation for smooth gradients and edges</li>
     *   <li>This produces significantly sharper thumbnails than direct resize</li>
     * </ul>
     */
    private BufferedImage resizeImageSquare(BufferedImage originalImage, int targetDimension) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // Determine crop region (center crop to square)
        int cropSize = Math.min(originalWidth, originalHeight);
        int cropX = (originalWidth - cropSize) / 2;
        int cropY = (originalHeight - cropSize) / 2;

        // Crop to square
        BufferedImage croppedImage = originalImage.getSubimage(cropX, cropY, cropSize, cropSize);

        // Use progressive downscaling for better quality
        // This produces much better results for large scale-downs (e.g., 1024 -> 64)
        BufferedImage currentImage = croppedImage;
        int currentSize = cropSize;

        // Progressively halve the image until we're close to target size
        while (currentSize > targetDimension * 2) {
            int newSize = currentSize / 2;
            
            BufferedImage tempImage = new BufferedImage(newSize, newSize, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = tempImage.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            g.drawImage(currentImage, 0, 0, newSize, newSize, null);
            g.dispose();
            
            currentImage = tempImage;
            currentSize = newSize;
        }

        // Final resize to exact target dimension
        BufferedImage resizedImage = new BufferedImage(targetDimension, targetDimension, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();

        // Use BICUBIC interpolation for maximum quality
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

        g.drawImage(currentImage, 0, 0, targetDimension, targetDimension, null);
        g.dispose();

        log.debug("Image cropped and resized (high quality) from {}x{} to {}x{} (square thumbnail)", 
                originalWidth, originalHeight, targetDimension, targetDimension);
        return resizedImage;
    }

    /**
     * Processes image: resizes to max 1024x1024, converts to JPEG, and compresses.
     */
    private byte[] processImage(MultipartFile file) throws IOException {
        // Read original image
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        if (originalImage == null) {
            throw new IOException("Unable to read image file");
        }

        // Resize image to max 1024x1024 (maintaining aspect ratio)
        BufferedImage resizedImage = resizeImage(originalImage, maxDimension);

        // Convert to JPEG and compress
        return convertToJpeg(resizedImage, jpegQuality);
    }

    /**
     * Resizes image to maximum dimension while maintaining aspect ratio.
     */
    private BufferedImage resizeImage(BufferedImage originalImage, int maxDimension) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // If image is already smaller than max dimension, return original
        if (originalWidth <= maxDimension && originalHeight <= maxDimension) {
            return originalImage;
        }

        // Calculate new dimensions maintaining aspect ratio
        double scale = Math.min((double) maxDimension / originalWidth, (double) maxDimension / originalHeight);
        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);

        // Create resized image with high quality
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();
        
        // Use high-quality rendering hints
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g.dispose();

        log.debug("Image resized from {}x{} to {}x{}", originalWidth, originalHeight, newWidth, newHeight);
        return resizedImage;
    }

    /**
     * Converts BufferedImage to JPEG format with compression.
     */
    private byte[] convertToJpeg(BufferedImage image, float quality) throws IOException {
        // Convert to RGB if needed (JPEG doesn't support transparency)
        BufferedImage jpegImage = image;
        if (image.getType() != BufferedImage.TYPE_INT_RGB) {
            jpegImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = jpegImage.createGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
        }

        // Write JPEG with compression
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer available");
        }

        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
        }

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new javax.imageio.IIOImage(jpegImage, null, null), param);
        } finally {
            writer.dispose();
        }

        byte[] jpegBytes = baos.toByteArray();
        log.debug("Image converted to JPEG: {} bytes (quality: {})", jpegBytes.length, quality);
        return jpegBytes;
    }

    @Override
    public String getImageUrl(String imageKey) {
        // If baseUrl is configured, use it (CDN or public URL)
        if (baseUrl != null && !baseUrl.trim().isEmpty()) {
            return baseUrl.endsWith("/") 
                    ? baseUrl + imageKey 
                    : baseUrl + "/" + imageKey;
        }

        // Otherwise, generate presigned URL
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(imageKey)
                    .build();

            PresignedGetObjectRequest presignedRequest = getS3Presigner()
                    .presignGetObject(GetObjectPresignRequest.builder()
                            .signatureDuration(Duration.ofHours(presignedUrlExpirationHours))
                            .getObjectRequest(getObjectRequest)
                            .build());

            return presignedRequest.url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for image: {}", imageKey, e);
            // Fallback: return relative path
            return "/api/v1/users/{userId}/image/" + imageKey;
        }
    }

    @Override
    public byte[] getImageBytes(String imageKey) throws IOException {
        // Extract userId from imageKey (format: users/{userId}/{filename})
        String[] parts = imageKey.split("/");
        if (parts.length >= 2) {
            String userId = parts[1];
            return getImageBytes(userId, parts[parts.length - 1]);
        }
        throw new IOException("Invalid image key format: " + imageKey);
    }

    @Override
    public byte[] getImageBytes(String userId, String imageKey) throws IOException {
        // Construct full S3 key: users/{userId}/{imageKey}
        String fullKey = String.format("users/%s/%s", userId, imageKey);
        
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fullKey)
                    .build();

            try (InputStream inputStream = getS3Client().getObject(getObjectRequest)) {
                byte[] imageBytes = inputStream.readAllBytes();
                log.debug("Successfully retrieved image from S3: key={}, size={} bytes", 
                        fullKey, imageBytes.length);
                return imageBytes;
            }
        } catch (NoSuchKeyException e) {
            log.warn("Image not found in S3: key={}", fullKey);
            throw new IOException("Image not found: " + imageKey + " for user: " + userId, e);
        } catch (S3Exception e) {
            log.error("Failed to retrieve image from S3: key={}, error={}", fullKey, e.getMessage(), e);
            throw new IOException("Failed to retrieve image from S3: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteImage(String imageKey) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(imageKey)
                    .build();

            getS3Client().deleteObject(deleteObjectRequest);
            log.info("Image deleted from S3: key={}", imageKey);
            return true;
        } catch (S3Exception e) {
            log.error("Failed to delete image from S3: key={}, error={}", imageKey, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void validateImage(MultipartFile file) throws IllegalArgumentException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }

        if (file.getSize() > maxImageSize) {
            throw new IllegalArgumentException("Image size exceeds maximum allowed size: " + maxImageSize + " bytes");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid image format. Allowed formats: JPEG, PNG, GIF, WEBP");
        }
    }
}

