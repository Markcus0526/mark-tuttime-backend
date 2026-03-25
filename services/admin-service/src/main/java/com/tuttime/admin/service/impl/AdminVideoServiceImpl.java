package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.VideoRequest;
import com.tuttime.admin.dto.VideoResponse;
import com.tuttime.admin.rest.client.ContentServiceRestClient;
import com.tuttime.admin.service.AdminVideoService;
import com.tuttime.admin.service.VideoStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


/**
 * Admin Video Service Implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminVideoServiceImpl implements AdminVideoService {

	private final ContentServiceRestClient contentServiceRestClient;
	private final VideoStorageService videoStorageService;

	@Override
	public Page<VideoResponse> listVideos(Pageable pageable, String status) {
		return contentServiceRestClient.listVideos(pageable, status);
	}

	@Override
	public VideoResponse getVideo(Long id) {
		return contentServiceRestClient.getVideo(id);
	}

	@Override
	public VideoResponse createVideo(VideoRequest request, Long adminId) {
		return contentServiceRestClient.createVideo(request, adminId);
	}

	@Override
	public VideoResponse updateVideo(Long id, VideoRequest request) {
		// Get video first to extract old video and thumbnail keys for file deletion if they're being updated
		VideoResponse oldVideo = null;
		try {
			oldVideo = contentServiceRestClient.getVideo(id);
		} catch (Exception e) {
			log.warn("Failed to get video before update: id={}, error={}", id, e.getMessage());
			// Continue with update even if we can't get the old video
		}

		// Update video in content-service (database)
		VideoResponse updatedVideo = contentServiceRestClient.updateVideo(id, request);

		// Delete old video file from S3 if video URL was updated
		if (oldVideo != null) {
			String oldVideoUrl = oldVideo.getVideoUrl();
			String newVideoUrl = request.getVideoUrl() != null ? request.getVideoUrl() :
					(request.getS3FileKey() != null ? request.getS3FileKey() : null);

			// Only delete if video URL actually changed
			if (oldVideoUrl != null && newVideoUrl != null && !oldVideoUrl.equals(newVideoUrl)) {
				String oldVideoKey = extractVideoKeyFromUrl(oldVideoUrl);
				if (oldVideoKey != null) {
					boolean deleted = videoStorageService.deleteVideo(oldVideoKey);
					if (deleted) {
						log.info("Deleted old video file from S3: key={}, videoId={}", oldVideoKey, id);
					} else {
						log.warn("Failed to delete old video file from S3: key={}, videoId={}", oldVideoKey, id);
					}
				}
			}

			// Delete old thumbnail file from S3 if thumbnail was updated
			if (request.getThumbnailUrl() != null && !request.getThumbnailUrl().trim().isEmpty()) {
				String oldThumbnailUrl = oldVideo.getThumbnailUrl();
				String newThumbnailUrl = request.getThumbnailUrl();

				// Only delete if thumbnail actually changed
				if (oldThumbnailUrl != null && !oldThumbnailUrl.equals(newThumbnailUrl)) {
					String oldThumbnailKey = extractThumbnailKeyFromUrl(oldThumbnailUrl);
					if (oldThumbnailKey != null) {
						boolean deleted = videoStorageService.deleteThumbnail(oldThumbnailKey);
						if (deleted) {
							log.info("Deleted old thumbnail file from S3: key={}, videoId={}", oldThumbnailKey, id);
						} else {
							log.warn("Failed to delete old thumbnail file from S3: key={}, videoId={}", oldThumbnailKey, id);
						}
					}
				}
			}
		}

		return updatedVideo;
	}

	@Override
	public void deleteVideo(Long id) {
		// Get video first to extract video and thumbnail keys for file deletion
		VideoResponse video = null;
		try {
			video = contentServiceRestClient.getVideo(id);
		} catch (Exception e) {
			log.warn("Failed to get video before deletion: id={}, error={}", id, e.getMessage());
			// Continue with deletion even if we can't get the video
		}

		// Delete video from content-service (database)
		contentServiceRestClient.deleteVideo(id);

		// Delete video and thumbnail files from S3 if they exist
		if (video != null) {
			// Extract and delete video file
			String videoUrl = video.getVideoUrl();
			if (videoUrl != null && !videoUrl.trim().isEmpty()) {
				String videoKey = extractVideoKeyFromUrl(videoUrl);
				if (videoKey != null) {
					boolean deleted = videoStorageService.deleteVideo(videoKey);
					if (deleted) {
						log.info("Deleted video file from S3: key={}, videoId={}", videoKey, id);
					} else {
						log.warn("Failed to delete video file from S3: key={}, videoId={}", videoKey, id);
					}
				}
			}

			// Extract and delete thumbnail file
			String thumbnailUrl = video.getThumbnailUrl();
			if (thumbnailUrl != null && !thumbnailUrl.trim().isEmpty()) {
				String thumbnailKey = extractThumbnailKeyFromUrl(thumbnailUrl);
				if (thumbnailKey != null) {
					boolean deleted = videoStorageService.deleteThumbnail(thumbnailKey);
					if (deleted) {
						log.info("Deleted thumbnail file from S3: key={}, videoId={}", thumbnailKey, id);
					} else {
						log.warn("Failed to delete thumbnail file from S3: key={}, videoId={}", thumbnailKey, id);
					}
				}
			}
		}
	}

	/**
	 * Extracts the S3 video key from a video URL.
	 * Handles both S3 keys and full URLs (presigned URLs, CDN URLs).
	 *
	 * @param videoUrl Video URL or S3 key
	 * @return Extracted S3 key, or null if extraction fails
	 */
	private String extractVideoKeyFromUrl(String videoUrl) {
		if (videoUrl == null || videoUrl.trim().isEmpty()) {
			return null;
		}

		String normalized = videoUrl.trim();

		// If it's already an S3 key (doesn't start with http), return as-is
		if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
			// Remove leading slashes
			while (normalized.startsWith("/")) {
				normalized = normalized.substring(1);
			}
			// Check if it looks like a video key
			if (normalized.contains("videos/") || normalized.startsWith("videos/")) {
				return normalized;
			}
			return null;
		}

		// It's a full URL - try to extract the key
		try {
			java.net.URI uri = new java.net.URI(normalized);
			String path = uri.getPath();
			if (path != null && path.startsWith("/")) {
				path = path.substring(1);
			}
			// Check if path looks like a video key
			if (path != null && (path.contains("videos/") || path.startsWith("videos/"))) {
				return path;
			}
		} catch (Exception e) {
			log.debug("Failed to parse video URL to extract key: {}", videoUrl, e);
		}

		return null;
	}

	/**
	 * Extracts the S3 thumbnail key from a thumbnail URL.
	 * Handles both S3 keys and full URLs (presigned URLs, CDN URLs).
	 *
	 * @param thumbnailUrl Thumbnail URL or S3 key
	 * @return Extracted S3 key, or null if extraction fails
	 */
	private String extractThumbnailKeyFromUrl(String thumbnailUrl) {
		if (thumbnailUrl == null || thumbnailUrl.trim().isEmpty()) {
			return null;
		}

		String normalized = thumbnailUrl.trim();

		// If it's already an S3 key (doesn't start with http), return as-is
		if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
			// Remove leading slashes
			while (normalized.startsWith("/")) {
				normalized = normalized.substring(1);
			}
			// Check if it looks like a thumbnail key
			if (normalized.contains("thumbnails/") || normalized.startsWith("thumbnails/")) {
				return normalized;
			}
			return null;
		}

		// It's a full URL - try to extract the key
		try {
			java.net.URI uri = new java.net.URI(normalized);
			String path = uri.getPath();
			if (path != null && path.startsWith("/")) {
				path = path.substring(1);
			}
			// Check if path looks like a thumbnail key
			if (path != null && (path.contains("thumbnails/") || path.startsWith("thumbnails/"))) {
				return path;
			}
		} catch (Exception e) {
			log.debug("Failed to parse thumbnail URL to extract key: {}", thumbnailUrl, e);
		}

		return null;
	}

	@Override
	public VideoResponse approveVideo(Long id, Long adminId) {
		return contentServiceRestClient.approveVideo(id, adminId);
	}

	@Override
	public VideoResponse rejectVideo(Long id, Long adminId, com.tuttime.admin.dto.RejectRequest request) {
		return contentServiceRestClient.rejectVideo(id, adminId, request);
	}

	@Override
	public VideoResponse publishVideo(Long id) {
		return contentServiceRestClient.publishVideo(id);
	}

	@Override
	public VideoResponse scheduleVideo(Long id, com.tuttime.admin.dto.ScheduleVideoRequest request) {
		return contentServiceRestClient.scheduleVideo(id, request);
	}
}

