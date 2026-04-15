package com.tuttime.restaurant.service.impl;

import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.restaurant.dto.MediaDto;
import com.tuttime.restaurant.entity.Media;
import com.tuttime.restaurant.entity.enums.MediaType;
import com.tuttime.restaurant.mapper.MediaMapper;
import com.tuttime.restaurant.repository.MediaRepository;
import com.tuttime.restaurant.service.FileStorageService;
import com.tuttime.restaurant.service.MediaService;
import com.tuttime.restaurant.service.StaffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

	private final MediaRepository mediaRepository;
	private final MediaMapper mediaMapper;
	private final StaffService staffService;
	private final FileStorageService fileStorageService;

	@Override
	@Transactional(readOnly = true)
	public List<MediaDto.MediaResponse> getMediaByEntity(UUID entityId, MediaType mediaType) {
		// If mediaType is null, the query returns all media for the entity
		return mediaRepository.findAllByEntityIdAndOptionalMediaType(entityId, mediaType)
				.stream()
				.map(mediaMapper::toResponse)
				.toList(); // Simplified for Java 16+
	}


	@Override
	@Transactional
	public MediaDto.MediaResponse createMedia(MediaDto.MediaRequest request, MultipartFile file, UUID currentUserId, String userRoles) {

		// 1. Validation
		checkMedia(file, request.mediaType());

		// This was line 54: If staffService is still null,
		// verify StaffServiceImpl has @Service and is in a scanned package.
		staffService.validatePermissions(request.entityId(), currentUserId, userRoles);

		// 2. File Storage
		String storedFileUrl;
		try {
			storedFileUrl = fileStorageService.storeFile(file);
		} catch (IOException e) {
			throw new BusinessException("Failed to store file: " + e.getMessage(), e);
		}

		// 3. Database Save
		Media media = mediaMapper.toEntity(request);
		media.setUrl(storedFileUrl);
		media.setDeleted(false);

		if (Boolean.TRUE.equals(media.getPrimaryEntity())) {
			mediaRepository.updatePrimaryEntityByEntityTypeAndEntityIdAndDeletedFalse(media.getEntityId(), false);
		}

		return mediaMapper.toResponse(mediaRepository.save(media));
	}

	@Override
	@Transactional
	public MediaDto.MediaResponse updateMedia(UUID mediaId, MediaDto.MediaRequest request, MultipartFile file, UUID currentUserId, String userRoles) {
		// 1. Fetch and Validate
		Media media = mediaRepository.findByIdAndDeletedFalse(mediaId)
				.orElseThrow(() -> new InvalidRequestException("Media not found: " + mediaId));

		staffService.validatePermissions(media.getEntityId(), currentUserId, userRoles);

		String oldFileUrl = media.getUrl();
		String newFileUrl = oldFileUrl;

		// 2. Handle File Update
		if (file != null && !file.isEmpty()) {
			checkMedia(file, request.mediaType());
			try {
				newFileUrl = fileStorageService.storeFile(file);
			} catch (IOException e) {
				throw new BusinessException("Failed to upload new file", e);
			}
		}

		// 3. Update DB Record (Atomic Transaction)
		MediaDto.MediaResponse response = performUpdate(media, request, newFileUrl);

		// 4. Cleanup: Delete old file if it was replaced
		if (!newFileUrl.equals(oldFileUrl)) {
			try {
				fileStorageService.deleteFile(oldFileUrl);
			} catch (Exception e) {
				// Log warning but don't fail the request since DB update succeeded
				log.warn("Failed to delete orphaned file: {}", oldFileUrl);
			}
		}

		return response;
	}

	@Transactional
	protected MediaDto.MediaResponse performUpdate(Media media, MediaDto.MediaRequest request, String fileUrl) {
		// Sync request data to entity
		mediaMapper.updateEntityFromRequest(request, media);
		media.setUrl(fileUrl);

		// Handle primary entity logic
		if (Boolean.TRUE.equals(request.primaryEntity())) {
			mediaRepository.updatePrimaryEntityByEntityTypeAndEntityIdAndDeletedFalse(media.getEntityId(), false);
		}

		return mediaMapper.toResponse(mediaRepository.save(media));
	}

	@Override
	@Transactional
	public void deleteMedia(UUID mediaId, UUID currentUserId, String userRoles) {
		Media media = mediaRepository.findByIdAndDeletedFalse(mediaId)
				.orElseThrow(() -> new InvalidRequestException("Media not found: " + mediaId));

		// 1. Centralized permission check
		staffService.validatePermissions(media.getEntityId(), currentUserId, userRoles);

		// 2. Soft delete
		media.setDeleted(true);
		mediaRepository.save(media);

		// Optional: If you want to delete the actual file from storage,
		// call fileStorageService.deleteFile(media.getUrl()) here.
	}

	@Override
	@Transactional
	public void deleteAllByEntityId(UUID entityId) {
		mediaRepository.deleteAllByEntityId(entityId);
	}

	@Override
	public String getMapCanvas(UUID entityId) {
		Media media = mediaRepository.findFirstByEntityIdAndMediaTypeAndDeletedFalse(
				entityId,
				MediaType.CANVAS
		).orElseThrow(() -> new InvalidRequestException("Primary SVG map not found for entity: " + entityId));

		return media.getSvgContent();
	}

	@Override
	@Transactional
	public void setOrUpdateMapCanvas(UUID entityId, String svgContent, UUID currentUserId, String userRoles) {
		// 1. Centralized Permission Check (Reuse existing logic)
		staffService.validatePermissions(entityId, currentUserId, userRoles);

		// 2. Fetch existing primary map or create a new instance
		Media media = mediaRepository.findPrimaryMapByEntityAndDeletedFalse(entityId)
				.orElseGet(() -> {
					Media newMedia = new Media();
					newMedia.setEntityId(entityId);
					newMedia.setMediaType(MediaType.CANVAS);
					newMedia.setMapEntity(true);
					newMedia.setDeleted(false);
					return newMedia;
				});

		// 3. Update the content
		media.setSvgContent(svgContent);

		// 4. Save (Hibernate handles Update vs Insert automatically)
		mediaRepository.save(media);
	}


	private void checkMedia(MultipartFile file, MediaType mediaType) {
		// Validate file size (e.g., max 5MB)
		long maxFileSize = 5 * 1024 * 1024; // 5 MB
		if (file.getSize() > maxFileSize) {
			throw new InvalidRequestException("File size exceeds the maximum allowed limit of 5 MB");
		}

		// Validate file type using Apache Tika
		Tika tika = new Tika();
		String detectedType;
		try {
			detectedType = tika.detect(file.getInputStream());
		} catch (IOException e) {
			throw new InvalidRequestException("Could not detect file type");
		}

		// Allowed MIME types based on mediaType parameter
		List<String> allowedMimeTypes = switch (mediaType) {
			case MediaType.IMAGE -> List.of("image/jpeg", "image/png", "image/gif", "image/svg+xml");
			case MediaType.VIDEO -> List.of("video/mp4", "video/webm", "video/ogg");
			case MediaType.CANVAS -> List.of("image/canvas+xml");
			default -> throw new InvalidRequestException("Unsupported media type");
		};

		if (!allowedMimeTypes.contains(detectedType)) {
			throw new InvalidRequestException("Invalid file type: " + detectedType);
		}

		// Optional: further validate SVG content if mediaType == SVG
		if (mediaType == MediaType.CANVAS) {
			try {
				String svgContent = new String(file.getBytes(), StandardCharsets.UTF_8);
				if (!isValidSvg(svgContent)) {
					throw new InvalidRequestException("Invalid or unsafe Canvas content");
				}
			} catch (IOException e) {
				throw new InvalidRequestException("Failed to read Canvas content");
			}
		}
	}

	/**
	 * Simple SVG validation example.
	 * You should use a library or custom sanitizer to remove scripts and unsafe elements.
	 */
	private boolean isValidSvg(String svgContent) {
		// Basic check: SVG must start with <svg and end with </svg>
		if (!svgContent.trim().startsWith("<svg") || !svgContent.trim().endsWith("</svg>")) {
			return false;
		}
		// Additional checks can be added here, e.g., disallow <script> tags
		return !svgContent.toLowerCase().contains("<script");
	}
}