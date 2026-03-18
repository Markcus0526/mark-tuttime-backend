package com.tuttime.user.service.impl;

import com.tuttime.user.service.ImageStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Local File System Image Storage Service Implementation
 * 
 * <p>Stores images in the local file system. For production, consider using
 * S3 or other cloud storage solutions.
 */
@Slf4j
@Service
public class LocalImageStorageServiceImpl implements ImageStorageService {

    @Value("${app.image.storage.path:./uploads/images}")
    private String storagePath;

    @Value("${app.image.max-size:5242880}") // 5MB default
    private long maxImageSize;

    @Value("${app.image.max-dimension:1024}") // Max width/height in pixels
    private int maxDimension;

    @Value("${app.image.quality:0.85}") // JPEG quality (0.0 to 1.0)
    private float jpegQuality;

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    @Override
    public String uploadImage(MultipartFile file, String userId) throws IOException {
        validateImage(file);

        // Create storage directory if it doesn't exist
        Path storageDir = Paths.get(storagePath, "users", userId);
        Files.createDirectories(storageDir);

        // Generate unique image key
        String imageKey = UUID.randomUUID().toString() + ".jpg";

        // Process image: resize, convert to JPEG, compress
        byte[] processedImage = processImage(file);

        // Save processed image
        Path filePath = storageDir.resolve(imageKey);
        Files.write(filePath, processedImage);

        log.info("Image uploaded and processed for user {}: key={}, size={} bytes", 
                userId, imageKey, processedImage.length);

        return imageKey;
    }

    /**
     * Processes image: resizes to max 1024x1024, converts to JPEG, and compresses.
     * 
     * @param file Original image file
     * @return Processed image bytes (JPEG format)
     * @throws IOException if image processing fails
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
     * 
     * @param originalImage Original image
     * @param maxDimension Maximum width or height in pixels
     * @return Resized image
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
     * 
     * @param image Image to convert
     * @param quality JPEG quality (0.0 to 1.0)
     * @return JPEG image bytes
     * @throws IOException if conversion fails
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
        // Extract userId from imageKey path or use a different approach
        // For now, we'll need to store userId with imageKey or extract from path
        // This is a simplified implementation - in production, you might want to store metadata
        
        // If imageKey contains path info, extract it
        // Otherwise, we'll need to reconstruct the URL
        // For local storage: /api/v1/users/{userId}/image/{imageKey}
        
        // Since we don't have userId in the key, we'll return a relative path
        // The controller will need to provide userId when constructing the full URL
        return "/api/v1/users/{userId}/image/" + imageKey;
    }

    @Override
    public byte[] getImageBytes(String imageKey) throws IOException {
        // Extract userId from imageKey or use a different approach
        // For now, we'll need to search or store mapping
        // This is a simplified implementation
        
        // Search in all user directories (not efficient, but works for now)
        // In production, you might want to store imageKey -> userId mapping
        Path storageBaseDir = Paths.get(storagePath, "users");
        
        if (!Files.exists(storageBaseDir)) {
            throw new IOException("Storage directory not found");
        }

        // Search for the image in all user directories
        return Files.walk(storageBaseDir)
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().equals(imageKey))
                .findFirst()
                .map(path -> {
                    try {
                        return Files.readAllBytes(path);
                    } catch (IOException e) {
                        log.error("Failed to read image: {}", path, e);
                        return null;
                    }
                })
                .orElseThrow(() -> new IOException("Image not found: " + imageKey));
    }
    
    @Override
    public byte[] getImageBytes(String userId, String imageKey) throws IOException {
        // Construct path directly using userId (more efficient)
        Path imagePath = Paths.get(storagePath, "users", userId, imageKey);
        
        log.debug("Looking for image at path: {} for userId: {}, imageKey: {}", imagePath, userId, imageKey);
        
        if (!Files.exists(imagePath)) {
            log.warn("Image file not found at path: {} for userId: {}, imageKey: {}", imagePath, userId, imageKey);
            throw new IOException("Image not found: " + imageKey + " for user: " + userId + " at path: " + imagePath);
        }
        
        if (!Files.isRegularFile(imagePath)) {
            log.warn("Path exists but is not a regular file: {} for userId: {}, imageKey: {}", imagePath, userId, imageKey);
            throw new IOException("Path is not a regular file: " + imagePath);
        }
        
        try {
            byte[] imageBytes = Files.readAllBytes(imagePath);
            log.debug("Successfully read image from path: {} for userId: {}, imageKey: {}, size: {} bytes", 
                    imagePath, userId, imageKey, imageBytes.length);
            return imageBytes;
        } catch (IOException e) {
            log.error("Failed to read image from path: {} for userId: {}, imageKey: {}", imagePath, userId, imageKey, e);
            throw new IOException("Failed to read image: " + imageKey + " for user: " + userId, e);
        }
    }

    @Override
    public boolean deleteImage(String imageKey) {
        try {
            Path storageBaseDir = Paths.get(storagePath, "users");
            
            if (!Files.exists(storageBaseDir)) {
                return false;
            }

            // Search for the image in all user directories
            Path imagePath = Files.walk(storageBaseDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(imageKey))
                    .findFirst()
                    .orElse(null);

            if (imagePath != null) {
                return Files.deleteIfExists(imagePath);
            }

            return false;
        } catch (Exception e) {
            log.error("Failed to delete image: {}", imageKey, e);
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

