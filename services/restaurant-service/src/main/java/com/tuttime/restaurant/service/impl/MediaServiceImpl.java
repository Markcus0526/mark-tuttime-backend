package com.tuttime.restaurant.service.impl;

import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.restaurant.dto.MediaDto;
import com.tuttime.restaurant.entity.Media;
import com.tuttime.restaurant.entity.enums.MediaType;
import com.tuttime.restaurant.mapper.MediaMapper;
import com.tuttime.restaurant.repository.MediaRepository;
import com.tuttime.restaurant.service.MediaService;
import com.tuttime.restaurant.service.StaffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

	private final MediaRepository mediaRepository;
	private final MediaMapper mediaMapper;
	private final StaffService staffService;

	@Value("${app.media.local.upload-dir}")
	private String uploadDir;

	@Value("${app.media.local.upload-url}")
	private String uploadUrl;

	@Value("${app.media.max-size}")
	private int imageMaxSize;

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
		checkMedia(file, request.mediaType());

		staffService.validatePermissions(request.entityId(), currentUserId, userRoles);

		// 1. Save physical file
		String fileName = saveFile(file);

		if (request.primaryEntity()) {
			mediaRepository.resetPrimaryStatus(request.entityId());
		}

		// 2. Map metadata
		Media media = Media.builder()
				.entityId(request.entityId())
				.url(uploadUrl + fileName) // Virtual path served by WebMvcConfig
				.mediaType(detectMediaType(file))
				.primaryEntity(request.primaryEntity())
				.altText(request.altText())
				.build();

		return mediaMapper.toResponse(mediaRepository.save(media));
	}

	@Override
	@Transactional
	public MediaDto.MediaResponse updateMedia(UUID mediaId, MediaDto.MediaRequest request, MultipartFile file, UUID currentUserId, String userRoles) {
		Media existing = mediaRepository.findById(mediaId)
				.orElseThrow(() -> new RuntimeException("Media not found"));

		checkMedia(file, request.mediaType());

		staffService.validatePermissions(existing.getEntityId(), currentUserId, userRoles);

		// 1. Handle File Replacement if a new file is provided
		if (file != null && !file.isEmpty()) {
			deletePhysicalFile(existing.getUrl()); // Delete old file from VPS
			String newFileName = saveFile(file);   // Save new file
			existing.setUrl(uploadUrl + newFileName);
			existing.setMediaType(detectMediaType(file));
		}

		if (request.primaryEntity()) {
			mediaRepository.resetPrimaryStatus(request.entityId());
		}

		// 2. Update Metadata
		existing.setAltText(request.altText());
		existing.setPrimaryEntity(request.primaryEntity());

		return mediaMapper.toResponse(mediaRepository.save(existing));
	}

	@Override
	@Transactional
	public void deleteMedia(UUID mediaId, UUID currentUserId, String userRoles) {
		Media media = mediaRepository.findById(mediaId)
				.orElseThrow(() -> new RuntimeException("Media not found"));

		staffService.validatePermissions(media.getEntityId(), currentUserId, userRoles);

		deletePhysicalFile(media.getUrl()); // Physical cleanup

		UUID entityId = media.getEntityId();
		boolean wasPrimary = media.getPrimaryEntity();

		mediaRepository.delete(media);      // Database cleanup

		if (wasPrimary) {
			promoteNewPrimary(entityId);
		}
	}

	@Override
	@Transactional
	public void deleteAllByEntityId(UUID entityId) {
		mediaRepository.deleteAllByEntityId(entityId);
	}

	private String saveFile(MultipartFile file) {
		String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
		try {
			Path targetPath = Paths.get(uploadDir).resolve(fileName);
			Files.createDirectories(targetPath.getParent());
			Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
			return fileName;
		} catch (IOException e) {
			throw new RuntimeException("Could not store file on VPS", e);
		}
	}

	private void deletePhysicalFile(String virtualPath) {
		// Converts "/media/filename.jpg" -> "/var/www/menu-media/filename.jpg"
		String fileName = virtualPath.replace(uploadUrl, "");
		try {
			Files.deleteIfExists(Paths.get(uploadDir).resolve(fileName));
		} catch (IOException e) {
			// Log warning but don't fail transaction if file is already gone
		}
	}

	private MediaType detectMediaType(MultipartFile file) {
		String contentType = file.getContentType();
		if (contentType != null && contentType.startsWith("video")) {
			return MediaType.VIDEO;
		}
		return MediaType.IMAGE;
	}

	private void promoteNewPrimary(UUID entityId) {
		// Find the first available image for this owner
		mediaRepository.findFirstByEntityIdOrderByCreatedAtAsc(entityId)
				.ifPresent(newPrimary -> {
					newPrimary.setPrimaryEntity(true);
					mediaRepository.save(newPrimary);
				});
	}

	private void checkMedia(MultipartFile file, MediaType mediaType) {
		if (file.getSize() > imageMaxSize) {
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
			default -> throw new InvalidRequestException("Unsupported media type");
		};

		if (!allowedMimeTypes.contains(detectedType)) {
			throw new InvalidRequestException("Invalid file type: " + detectedType);
		}
	}
}