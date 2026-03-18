package com.tuttime.user.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Image Storage Service Interface
 * 
 * <p>This interface defines the contract for image storage operations.
 * Implementations can use local file system, S3, or other storage solutions.
 * 
 * <p>All images are processed before storage:
 * <ul>
 *   <li>Resized to maximum 1024x1024 pixels (maintaining aspect ratio)</li>
 *   <li>Converted to JPEG format</li>
 *   <li>Compressed for optimal file size</li>
 * </ul>
 */
public interface ImageStorageService {
    
    /**
     * Uploads an image file and returns the image key.
     * 
     * <p>The image is processed (resized, converted to JPEG, compressed) before storage.
     * 
     * @param file Image file to upload
     * @param userId User ID for organizing images
     * @return Image key (unique identifier for the stored image)
     * @throws IOException if file upload fails
     * @throws IllegalArgumentException if file is invalid
     */
    String uploadImage(MultipartFile file, String userId) throws IOException;
    
    /**
     * Gets the URL for an image by its key.
     * 
     * @param imageKey Image key (unique identifier)
     * @return Image URL
     */
    String getImageUrl(String imageKey);
    
    /**
     * Deletes an image by its key.
     * 
     * @param imageKey Image key (unique identifier)
     * @return true if deletion was successful
     */
    boolean deleteImage(String imageKey);
    
    /**
     * Validates image file (size, format).
     * 
     * @param file Image file to validate
     * @throws IllegalArgumentException if file is invalid
     */
    void validateImage(MultipartFile file) throws IllegalArgumentException;
    
    /**
     * Retrieves image bytes by image key (for backward compatibility).
     * 
     * @param imageKey Image key (unique identifier)
     * @return Image bytes
     * @throws IOException if image retrieval fails
     */
    byte[] getImageBytes(String imageKey) throws IOException;
    
    /**
     * Retrieves image bytes by userId and image key (more efficient).
     * 
     * @param userId User ID
     * @param imageKey Image key (unique identifier)
     * @return Image bytes
     * @throws IOException if image retrieval fails
     */
    byte[] getImageBytes(String userId, String imageKey) throws IOException;
}

