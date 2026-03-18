package com.tuttime.admin.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Video Storage Service Interface
 * 
 * <p>This interface defines the contract for video storage operations.
 * Implementations can use S3 or other storage solutions.
 */
public interface VideoStorageService {
    
    /**
     * Uploads a video file and returns the video key.
     * 
     * @param file Video file to upload
     * @return Video key (unique identifier for the stored video)
     * @throws IOException if file upload fails
     * @throws IllegalArgumentException if file is invalid
     */
    String uploadVideo(MultipartFile file) throws IOException;
    
    /**
     * Gets the URL for a video by its key.
     * 
     * @param videoKey Video key (unique identifier)
     * @return Video URL
     */
    String getVideoUrl(String videoKey);
    
    /**
     * Validates video file (size, format).
     * 
     * @param file Video file to validate
     * @throws IllegalArgumentException if file is invalid
     */
    void validateVideo(MultipartFile file) throws IllegalArgumentException;
    
    /**
     * Uploads a thumbnail image file and returns the thumbnail key.
     * 
     * @param file Thumbnail image file to upload
     * @return Thumbnail key (unique identifier for the stored thumbnail)
     * @throws IOException if file upload fails
     * @throws IllegalArgumentException if file is invalid
     */
    String uploadThumbnail(MultipartFile file) throws IOException;
    
    /**
     * Gets the URL for a thumbnail by its key.
     * 
     * @param thumbnailKey Thumbnail key (unique identifier)
     * @return Thumbnail URL
     */
    String getThumbnailUrl(String thumbnailKey);
    
    /**
     * Validates thumbnail image file (size, format).
     * 
     * @param file Thumbnail image file to validate
     * @throws IllegalArgumentException if file is invalid
     */
    void validateThumbnail(MultipartFile file) throws IllegalArgumentException;
    
    /**
     * Deletes a video file from storage by its key.
     * 
     * @param videoKey Video key (unique identifier)
     * @return true if the video was deleted successfully, false otherwise
     */
    boolean deleteVideo(String videoKey);
    
    /**
     * Deletes a thumbnail file from storage by its key.
     * 
     * @param thumbnailKey Thumbnail key (unique identifier)
     * @return true if the thumbnail was deleted successfully, false otherwise
     */
    boolean deleteThumbnail(String thumbnailKey);
}

