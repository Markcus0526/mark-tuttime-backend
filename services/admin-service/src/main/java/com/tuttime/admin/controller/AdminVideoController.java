package com.tuttime.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.admin.dto.RejectRequest;
import com.tuttime.admin.dto.ScheduleVideoRequest;
import com.tuttime.admin.dto.VideoRequest;
import com.tuttime.admin.dto.VideoResponse;
import com.tuttime.admin.service.AdminAuditService;
import com.tuttime.admin.service.AdminVideoService;
import com.tuttime.admin.service.VideoStorageService;
import com.tuttime.common.dto.ApiResponse;
import com.tuttime.common.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Admin Video Management Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/videos")
@RequiredArgsConstructor
@Tag(name = "Admin Video Management", description = "Video management APIs for admins")
public class AdminVideoController {

	private final AdminVideoService videoService;
	private final AdminAuditService auditService;
	private final VideoStorageService videoStorageService;
	private final ObjectMapper objectMapper;

	@GetMapping
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "List all videos", description = "Get paginated list of all videos with optional status filter")
	public ResponseEntity<ApiResponse<Map<String, Object>>> listVideos(
			@PageableDefault(size = 50) Pageable pageable,
			@RequestParam(required = false) String status,
			@AuthenticationPrincipal Long adminId) {

		Page<VideoResponse> videos = videoService.listVideos(pageable, status);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.VIDEO_VIEWED,
				"VIDEO", null, "Listed all videos");

		// Convert Page to Map to ensure proper JSON serialization
		// Spring's PageImpl serialization can be unstable, so we manually create a Map
		Map<String, Object> pageMap = new java.util.HashMap<>();
		pageMap.put("content", videos.getContent());
		pageMap.put("totalElements", videos.getTotalElements());
		pageMap.put("totalPages", videos.getTotalPages());
		pageMap.put("number", videos.getNumber());
		pageMap.put("size", videos.getSize());
		pageMap.put("numberOfElements", videos.getNumberOfElements());
		pageMap.put("first", videos.isFirst());
		pageMap.put("last", videos.isLast());
		pageMap.put("empty", videos.isEmpty());
		pageMap.put("pageable", Map.of(
				"pageNumber", videos.getPageable().getPageNumber(),
				"pageSize", videos.getPageable().getPageSize(),
				"paged", videos.getPageable().isPaged()
		));

		return ResponseEntity.ok(ApiResponse.success("Videos retrieved successfully", pageMap));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "Get video details", description = "Get details of a specific video")
	public ResponseEntity<ApiResponse<VideoResponse>> getVideo(
			@PathVariable Long id,
			@AuthenticationPrincipal Long adminId) {

		VideoResponse video = videoService.getVideo(id);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.VIDEO_VIEWED,
				"VIDEO", id, "Viewed video details");

		return ResponseEntity.ok(ApiResponse.success("Video retrieved successfully", video));
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
	@Operation(summary = "Create video (JSON)", description = "Create a new video with video URL (no file upload)")
	public ResponseEntity<ApiResponse<VideoResponse>> createVideoJson(
			@Valid @RequestBody VideoRequest request,
			@AuthenticationPrincipal Long adminId) {

		VideoResponse video = videoService.createVideo(request, adminId);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.VIDEO_CREATED,
				"VIDEO", video.getVideoId(),
				String.format("Created video: %s", request.getTitle()));

		return ResponseEntity.ok(ApiResponse.success("Video created successfully", video));
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
	@Operation(summary = "Create/upload video with file", description = "Create a new video with direct file upload (similar to image upload)")
	public ResponseEntity<ApiResponse<VideoResponse>> createVideoWithFile(
			@RequestParam(value = "data", required = false) String dataJson,
			@RequestPart(value = "video", required = false) MultipartFile videoFile,
			@RequestPart(value = "thumbnail", required = false) MultipartFile thumbnailFile,
			@AuthenticationPrincipal Long adminId) {

		log.info("Creating video with file upload for adminId: {}, dataJson present: {}, videoFile present: {}, thumbnailFile present: {}",
				adminId, dataJson != null, videoFile != null, thumbnailFile != null);

		if (videoFile != null) {
			log.info("Video file details: fileName={}, size={}, contentType={}",
					videoFile.getOriginalFilename(), videoFile.getSize(), videoFile.getContentType());
		}

		if (thumbnailFile != null) {
			log.info("Thumbnail file details: fileName={}, size={}, contentType={}",
					thumbnailFile.getOriginalFilename(), thumbnailFile.getSize(), thumbnailFile.getContentType());
		}

		VideoRequest request = null;

		// Parse JSON data from form-data
		if (dataJson != null && !dataJson.trim().isEmpty()) {
			try {
				log.debug("Parsing JSON data from form-data: {}", dataJson);
				request = objectMapper.readValue(dataJson, VideoRequest.class);
			} catch (Exception e) {
				log.error("Failed to parse JSON data from form-data: {}", dataJson, e);
				throw new BusinessException("Invalid JSON format in data field: " + e.getMessage());
			}
		} else if (dataJson != null && dataJson.trim().isEmpty()) {
			// Handle case where data field is sent as empty string
			log.debug("Empty data field provided, creating empty request object");
			request = VideoRequest.builder().build();
		}

		// Handle video file upload if provided
		if (videoFile != null && !videoFile.isEmpty()) {
			try {
				log.info("Processing video file upload: fileName={}, size={}, contentType={}",
						videoFile.getOriginalFilename(), videoFile.getSize(), videoFile.getContentType());

				// Validate and upload video to S3
				videoStorageService.validateVideo(videoFile);
				String videoKey = videoStorageService.uploadVideo(videoFile);

				// Generate presigned URL once and save to database
				String videoUrl = videoStorageService.getVideoUrl(videoKey);

				// If request is null, create empty request
				if (request == null) {
					request = VideoRequest.builder().build();
				}

				// Set presigned URL (saved to database, returned directly without regeneration)
				request.setVideoUrl(videoUrl);

				log.info("Video uploaded to S3 and presigned URL generated: key={}, url={}", videoKey, videoUrl);
			} catch (IllegalArgumentException e) {
				throw new BusinessException(e.getMessage());
			} catch (Exception e) {
				log.error("Failed to upload video file", e);
				throw new BusinessException("Failed to upload video: " + e.getMessage());
			}
		}

		// Handle thumbnail file upload if provided
		if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
			try {
				log.info("Processing thumbnail file upload: fileName={}, size={}, contentType={}",
						thumbnailFile.getOriginalFilename(), thumbnailFile.getSize(), thumbnailFile.getContentType());

				// Validate and upload thumbnail to S3
				videoStorageService.validateThumbnail(thumbnailFile);
				String thumbnailKey = videoStorageService.uploadThumbnail(thumbnailFile);

				// Generate presigned URL once and save to database
				String thumbnailUrl = videoStorageService.getThumbnailUrl(thumbnailKey);

				// If request is null, create empty request
				if (request == null) {
					request = VideoRequest.builder().build();
				}

				// Set presigned URL (saved to database, returned directly without regeneration)
				request.setThumbnailUrl(thumbnailUrl);

				log.info("Thumbnail uploaded to S3 and presigned URL generated: key={}, url={}", thumbnailKey, thumbnailUrl);
			} catch (IllegalArgumentException e) {
				throw new BusinessException(e.getMessage());
			} catch (Exception e) {
				log.error("Failed to upload thumbnail file", e);
				throw new BusinessException("Failed to upload thumbnail: " + e.getMessage());
			}
		}

		// Validate that either videoUrl or s3FileKey is provided
		if (request == null) {
			throw new BusinessException("Either video data or video file must be provided. " +
					"Send 'data' field with JSON or 'video' file, or both.");
		}

		if ((request.getVideoUrl() == null || request.getVideoUrl().trim().isEmpty()) &&
				(request.getS3FileKey() == null || request.getS3FileKey().trim().isEmpty())) {
			throw new BusinessException("Either videoUrl (in data) or video file must be provided.");
		}

		VideoResponse video = videoService.createVideo(request, adminId);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.VIDEO_CREATED,
				"VIDEO", video.getVideoId(),
				String.format("Created video: %s", request.getTitle()));

		return ResponseEntity.ok(ApiResponse.success("Video created successfully", video));
	}

	@PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
	@Operation(summary = "Update video", description = "Update an existing video (JSON only)")
	public ResponseEntity<ApiResponse<VideoResponse>> updateVideo(
			@PathVariable Long id,
			@Valid @RequestBody VideoRequest request,
			@AuthenticationPrincipal Long adminId) {

		VideoResponse video = videoService.updateVideo(id, request);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.VIDEO_UPDATED,
				"VIDEO", id, "Updated video");

		return ResponseEntity.ok(ApiResponse.success("Video updated successfully", video));
	}

	@PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
	@Operation(summary = "Update video with file upload", description = "Update an existing video with file uploads (video and/or thumbnail)")
	public ResponseEntity<ApiResponse<VideoResponse>> updateVideoWithFile(
			@PathVariable Long id,
			@RequestParam(value = "data", required = false) String dataJson,
			@RequestPart(value = "video", required = false) MultipartFile videoFile,
			@RequestPart(value = "thumbnail", required = false) MultipartFile thumbnailFile,
			@AuthenticationPrincipal Long adminId) {

		log.info("Updating video with file upload: id={}, dataJson present: {}, videoFile present: {}, thumbnailFile present: {}",
				id, dataJson != null, videoFile != null, thumbnailFile != null);

		VideoRequest request = null;

		// Parse JSON data from form-data
		if (dataJson != null && !dataJson.trim().isEmpty()) {
			try {
				log.debug("Parsing JSON data from form-data: {}", dataJson);
				request = objectMapper.readValue(dataJson, VideoRequest.class);
			} catch (Exception e) {
				log.error("Failed to parse JSON data from form-data: {}", dataJson, e);
				throw new BusinessException("Invalid JSON format in data field: " + e.getMessage());
			}
		}

		// If no data JSON provided, fetch existing video to preserve fields
		if (request == null) {
			VideoResponse existingVideo = videoService.getVideo(id);
			request = VideoRequest.builder()
					.title(existingVideo.getTitle())
					.description(existingVideo.getDescription())
					.videoUrl(existingVideo.getVideoUrl())
					.thumbnailUrl(existingVideo.getThumbnailUrl())
					.externalLink(existingVideo.getExternalLink())
					.durationSeconds(existingVideo.getDurationSeconds())
					.status(existingVideo.getStatus())
					.build();
		}

		// Handle video file upload if provided
		if (videoFile != null && !videoFile.isEmpty()) {
			try {
				log.info("Processing video file upload for update: fileName={}, size={}, contentType={}",
						videoFile.getOriginalFilename(), videoFile.getSize(), videoFile.getContentType());

				// Validate and upload video to S3
				videoStorageService.validateVideo(videoFile);
				String videoKey = videoStorageService.uploadVideo(videoFile);

				// Generate presigned URL once and save to database
				String videoUrl = videoStorageService.getVideoUrl(videoKey);

				// Set presigned URL (saved to database, returned directly without regeneration)
				request.setVideoUrl(videoUrl);

				log.info("Video uploaded successfully for update: key={}, url={}", videoKey, videoUrl);
			} catch (IllegalArgumentException e) {
				throw new BusinessException(e.getMessage());
			} catch (Exception e) {
				log.error("Failed to upload video file for update", e);
				throw new BusinessException("Failed to upload video: " + e.getMessage());
			}
		}

		// Handle thumbnail file upload if provided
		if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
			try {
				log.info("Processing thumbnail file upload for update: fileName={}, size={}, contentType={}",
						thumbnailFile.getOriginalFilename(), thumbnailFile.getSize(), thumbnailFile.getContentType());

				// Validate and upload thumbnail to S3
				videoStorageService.validateThumbnail(thumbnailFile);
				String thumbnailKey = videoStorageService.uploadThumbnail(thumbnailFile);

				// Generate presigned URL once and save to database
				String thumbnailUrl = videoStorageService.getThumbnailUrl(thumbnailKey);

				// Set presigned URL (saved to database, returned directly without regeneration)
				request.setThumbnailUrl(thumbnailUrl);

				log.info("Thumbnail uploaded successfully for update: key={}, url={}", thumbnailKey, thumbnailUrl);
			} catch (IllegalArgumentException e) {
				throw new BusinessException(e.getMessage());
			} catch (Exception e) {
				log.error("Failed to upload thumbnail file for update", e);
				throw new BusinessException("Failed to upload thumbnail: " + e.getMessage());
			}
		}

		VideoResponse video = videoService.updateVideo(id, request);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.VIDEO_UPDATED,
				"VIDEO", id, "Updated video");

		return ResponseEntity.ok(ApiResponse.success("Video updated successfully", video));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Remove video", description = "Delete a video")
	public ResponseEntity<ApiResponse<String>> deleteVideo(
			@PathVariable Long id,
			@AuthenticationPrincipal Long adminId) {

		videoService.deleteVideo(id);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.VIDEO_DELETED,
				"VIDEO", id, "Deleted video");

		return ResponseEntity.ok(ApiResponse.success("Video deleted successfully"));
	}

	@PostMapping("/{id}/approve")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
	@Operation(summary = "Approve video", description = "Approve a pending video for payment")
	public ResponseEntity<ApiResponse<VideoResponse>> approveVideo(
			@PathVariable Long id,
			@AuthenticationPrincipal Long adminId) {

		VideoResponse video = videoService.approveVideo(id, adminId);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.VIDEO_APPROVED,
				"VIDEO", id, "Approved video");

		return ResponseEntity.ok(ApiResponse.success("Video approved successfully", video));
	}

	@PostMapping("/{id}/reject")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
	@Operation(summary = "Reject video", description = "Reject a pending video")
	public ResponseEntity<ApiResponse<VideoResponse>> rejectVideo(
			@PathVariable Long id,
			@Valid @RequestBody RejectRequest request,
			@AuthenticationPrincipal Long adminId) {

		VideoResponse video = videoService.rejectVideo(id, adminId, request);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.VIDEO_REJECTED,
				"VIDEO", id, "Rejected video: " + request.getReason());

		return ResponseEntity.ok(ApiResponse.success("Video rejected successfully", video));
	}

	@PostMapping("/{id}/publish")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
	@Operation(summary = "Publish video", description = "Publish an approved video to make it active")
	public ResponseEntity<ApiResponse<VideoResponse>> publishVideo(
			@PathVariable Long id,
			@AuthenticationPrincipal Long adminId) {

		VideoResponse video = videoService.publishVideo(id);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.VIDEO_PUBLISHED,
				"VIDEO", id, "Published video");

		return ResponseEntity.ok(ApiResponse.success("Video published successfully", video));
	}

	@PostMapping("/{id}/schedule")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
	@Operation(summary = "Schedule video", description = "Schedule a video for future publication")
	public ResponseEntity<ApiResponse<VideoResponse>> scheduleVideo(
			@PathVariable Long id,
			@Valid @RequestBody ScheduleVideoRequest request,
			@AuthenticationPrincipal Long adminId) {

		VideoResponse video = videoService.scheduleVideo(id, request);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.VIDEO_SCHEDULED,
				"VIDEO", id, "Scheduled video");

		return ResponseEntity.ok(ApiResponse.success("Video scheduled successfully", video));
	}
}

