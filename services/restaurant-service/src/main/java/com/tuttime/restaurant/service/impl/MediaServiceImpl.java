package com.tuttime.restaurant.service.impl;

import com.tuttime.restaurant.dto.MediaRequest;
import com.tuttime.restaurant.dto.MediaResponse;
import com.tuttime.restaurant.entity.Media;
import com.tuttime.restaurant.enums.EntityType;
import com.tuttime.restaurant.enums.MediaType;
import com.tuttime.restaurant.mapper.MediaMapper;
import com.tuttime.restaurant.repository.MediaRepository;
import com.tuttime.restaurant.service.FileStorageService;
import com.tuttime.restaurant.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaServiceImpl implements MediaService {

	private final MediaRepository mediaRepository;
	private final MediaMapper mediaMapper;
	private final FileStorageService fileStorageService;

	@Override
	public List<MediaResponse> getMediaByEntity(EntityType entityType, UUID entityId) {
		List<Media> medias = mediaRepository.findByEntityTypeAndEntityIdAndIsDeletedFalse(entityType, entityId);
		return medias.stream()
				.map(mediaMapper::toResponse)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public MediaResponse createMedia(MediaRequest request, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("File must not be empty");
		}

		String storedFileUrl;
		try {
			storedFileUrl = fileStorageService.storeFile(file);
		} catch (IOException e) {
			throw new RuntimeException("Failed to store file", e);
		}

		Media media = new Media();
		media.setEntityType(request.getEntityType());
		media.setEntityId(request.getEntityId());
		media.setUrl(storedFileUrl);
		media.setAltText(request.getAltText());
		media.setIsPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : false);
		media.setMediaType(request.getMediaType() != null ? request.getMediaType() : MediaType.IMAGE);
		media.setIsDeleted(false);
		media.setCreatedAt(Instant.now().atOffset(ZoneOffset.UTC));
		media.setUpdatedAt(Instant.now().atOffset(ZoneOffset.UTC));

		Media saved = mediaRepository.save(media);
		return mediaMapper.toResponse(saved);
	}

	@Override
	@Transactional
	public MediaResponse updateMedia(UUID mediaId, MediaRequest request) {
		Media existing = mediaRepository.findById(mediaId)
				.orElseThrow(() -> new IllegalArgumentException("Media not found: " + mediaId));

		mediaMapper.updateEntityFromRequest(request, existing);
		existing.setUpdatedAt(Instant.now().atOffset(ZoneOffset.UTC));

		Media updated = mediaRepository.save(existing);
		return mediaMapper.toResponse(updated);
	}

	@Override
	@Transactional
	public void deleteMedia(UUID mediaId) {
		Media media = mediaRepository.findById(mediaId)
				.orElseThrow(() -> new IllegalArgumentException("Media not found: " + mediaId));

		// Soft delete
		media.setIsDeleted(true);
		media.setDeletedAt(Instant.now().atOffset(ZoneOffset.UTC));
		media.setUpdatedAt(Instant.now().atOffset(ZoneOffset.UTC));
		mediaRepository.save(media);
	}

	@Override
	public String getPrimarySvgMap(EntityType entityType, UUID entityId) {
		Media media = mediaRepository.findFirstByEntityTypeAndEntityIdAndMediaTypeAndIsPrimaryAndIsDeletedFalse(
				entityType,
				entityId,
				MediaType.SVG,
				true
		).orElseThrow(() -> new IllegalArgumentException("Primary SVG map not found for entity: " + entityType + ", id: " + entityId));

		return media.getSvgContent();
	}

	@Override
	@Transactional
	public void setOrUpdateSvgMap(EntityType entityType, UUID entityId, String svgContent) {
		Optional<Media> existingMapOpt = mediaRepository.findPrimaryMapByEntityAndIsDeletedFalse(entityType, entityId);

		Media media;
		Instant now = Instant.now();
		if (existingMapOpt.isPresent()) {
			media = existingMapOpt.get();
			media.setSvgContent(svgContent);
			media.setUpdatedAt(now.atOffset(ZoneOffset.UTC));
		} else {
			media = Media.builder()
					.entityType(entityType)
					.entityId(entityId)
					.mediaType(MediaType.SVG) // SVG media type
					.svgContent(svgContent)
					.isMap(true)
					.isPrimary(true) // Mark as primary map if needed
					.createdAt(now.atOffset(ZoneOffset.UTC))
					.updatedAt(now.atOffset(ZoneOffset.UTC))
					.isDeleted(false)
					.build();
		}
		mediaRepository.save(media);
	}
}