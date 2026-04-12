package com.tuttime.restaurant.service.impl;

import com.tuttime.common.auth.enums.UserRoleKind;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.restaurant.dto.MediaRequest;
import com.tuttime.restaurant.dto.MediaResponse;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.Media;
import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.entity.enums.EntityType;
import com.tuttime.restaurant.entity.enums.MediaType;
import com.tuttime.restaurant.mapper.MediaMapper;
import com.tuttime.restaurant.repository.BranchRepository;
import com.tuttime.restaurant.repository.MediaRepository;
import com.tuttime.restaurant.repository.RestaurantRepository;
import com.tuttime.restaurant.service.FileStorageService;
import com.tuttime.restaurant.service.MediaService;
import com.tuttime.restaurant.service.RestaurantStaffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaServiceImpl implements MediaService {

	private final RestaurantStaffService restaurantStaffService;
	private final RestaurantRepository restaurantRepository;
	private final BranchRepository branchRepository;
	private final MediaRepository mediaRepository;
	private final MediaMapper mediaMapper;
	private final FileStorageService fileStorageService;

	@Override
	public List<MediaResponse> getMediaByEntity(EntityType entityType, UUID entityId) {
		List<Media> medias = mediaRepository.findAllByEntityTypeAndEntityIdAndDeletedFalse(entityType, entityId);
		return medias.stream()
				.map(mediaMapper::toResponse)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public MediaResponse createMedia(MediaRequest request, MultipartFile file, UUID currentUserId, String userRoles) {
		if (request.getEntityType() == EntityType.RESTAURANT) {
			Restaurant restaurant = restaurantRepository.findByRestaurantIdAndDeletedFalse(request.getEntityId())
					.orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + request.getEntityId()));

			boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
			boolean isOwner = false;
			if (!isAdmin) {
				isOwner = restaurantStaffService.validateRestaurantOwner(restaurant.getRestaurantId(), currentUserId);
			}
			if (!isAdmin && !isOwner) {
				throw new AccessDeniedException("You do not have permission to manage this restaurant.");
			}
		} else if (request.getEntityType() == EntityType.BRANCH) {
			Branch branch = branchRepository.findByBranchIdAndDeletedFalse(request.getEntityId())
					.orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + request.getEntityId()));

			boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
			boolean isOwner = false;
			boolean isManager = false;
			if (!isAdmin) {
				isOwner = restaurantStaffService.validateRestaurantOwner(branch.getRestaurantId(), currentUserId);
				if (!isOwner) isManager = restaurantStaffService.validateBranchManager(branch.getBranchId(), currentUserId);
			}
			if (!isAdmin && !isOwner && !isManager) {
				throw new AccessDeniedException("You do not have permission to manage this restaurant.");
			}
		}

		checkMedia(file, request.getMediaType());

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
		media.setPrimaryEntity(request.getPrimaryEntity() != null ? request.getPrimaryEntity() : false);
		media.setMediaType(request.getMediaType() != null ? request.getMediaType() : MediaType.IMAGE);
		media.setDeleted(false);

		Media saved = mediaRepository.save(media);
		return mediaMapper.toResponse(saved);
	}

	@Override
	@Transactional
	public MediaResponse updateMedia(UUID mediaId, MediaRequest request, UUID currentUserId, String userRoles) {
		Media media = mediaRepository.findByMediaIdAndDeletedFalse(mediaId)
				.orElseThrow(() -> new ResourceNotFoundException("Media not found: " + mediaId));

		if (media.getEntityType() == EntityType.RESTAURANT) {
			Restaurant restaurant = restaurantRepository.findByRestaurantIdAndDeletedFalse(media.getEntityId())
					.orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + media.getEntityId()));

			boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
			boolean isOwner = false;
			if (!isAdmin) {
				isOwner = restaurantStaffService.validateRestaurantOwner(restaurant.getRestaurantId(), currentUserId);
			}
			if (!isAdmin && !isOwner) {
				throw new AccessDeniedException("You do not have permission to manage this restaurant.");
			}
		} else if (media.getEntityType() == EntityType.BRANCH) {
			Branch branch = branchRepository.findByBranchIdAndDeletedFalse(media.getEntityId())
					.orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + media.getEntityId()));

			boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
			boolean isOwner = false;
			boolean isManager = false;
			if (!isAdmin) {
				isOwner = restaurantStaffService.validateRestaurantOwner(branch.getRestaurantId(), currentUserId);
				if (!isOwner) isManager = restaurantStaffService.validateBranchManager(branch.getBranchId(), currentUserId);
			}
			if (!isAdmin && !isOwner && !isManager) {
				throw new AccessDeniedException("You do not have permission to manage this restaurant.");
			}
		}

		mediaMapper.updateEntityFromRequest(request, media);
		media.setUpdatedAt(Instant.now().atOffset(ZoneOffset.UTC));

		Media updated = mediaRepository.save(media);
		return mediaMapper.toResponse(updated);
	}

	@Override
	@Transactional
	public void deleteMedia(UUID mediaId, UUID currentUserId, String userRoles) {
		Media media = mediaRepository.findByMediaIdAndDeletedFalse(mediaId)
				.orElseThrow(() -> new ResourceNotFoundException("Media not found: " + mediaId));

		if (media.getEntityType() == EntityType.RESTAURANT) {
			Restaurant restaurant = restaurantRepository.findByRestaurantIdAndDeletedFalse(media.getEntityId())
					.orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + media.getEntityId()));

			boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
			boolean isOwner = false;
			if (!isAdmin) {
				isOwner = restaurantStaffService.validateRestaurantOwner(restaurant.getRestaurantId(), currentUserId);
			}
			if (!isAdmin && !isOwner) {
				throw new AccessDeniedException("You do not have permission to manage this restaurant.");
			}
		} else if (media.getEntityType() == EntityType.BRANCH) {
			Branch branch = branchRepository.findByBranchIdAndDeletedFalse(media.getEntityId())
					.orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + media.getEntityId()));

			boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
			boolean isOwner = false;
			boolean isManager = false;
			if (!isAdmin) {
				isOwner = restaurantStaffService.validateRestaurantOwner(branch.getRestaurantId(), currentUserId);
				if (!isOwner) isManager = restaurantStaffService.validateBranchManager(branch.getBranchId(), currentUserId);
			}
			if (!isAdmin && !isOwner && !isManager) {
				throw new AccessDeniedException("You do not have permission to manage this restaurant.");
			}
		}

		// Soft delete
		media.setDeleted(true);
		mediaRepository.save(media);
	}

	@Override
	public boolean deleteAllByEntityId(EntityType entityType, UUID entityId) {
		mediaRepository.deleteAllByEntityTypeAndEntityId(entityType, entityId);
		return true;
	}

	@Override
	public String getPrimarySvgMap(EntityType entityType, UUID entityId) {
		Media media = mediaRepository.findFirstByEntityTypeAndEntityIdAndMediaTypeAndPrimaryEntityAndDeletedFalse(
				entityType,
				entityId,
				MediaType.SVG,
				true
		).orElseThrow(() -> new IllegalArgumentException("Primary SVG map not found for entity: " + entityType + ", id: " + entityId));

		return media.getSvgContent();
	}

	// TODO
	@Override
	@Transactional
	public void setOrUpdateSvgMap(EntityType entityType, UUID entityId, String svgContent, UUID currentUserId, String userRoles) {
		Optional<Media> existingMapOpt = mediaRepository.findPrimaryMapByEntityAndDeletedFalse(entityType, entityId);

		if (entityType == EntityType.RESTAURANT) {

		} else {

		}

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
					.mapEntity(true)
					.primaryEntity(true) // Mark as primary map if needed
					.build();
		}
		mediaRepository.save(media);
	}

	private void checkMedia(MultipartFile file, MediaType mediaType) {
		// Validate file size (e.g., max 5MB)
		long maxFileSize = 5 * 1024 * 1024; // 5 MB
		if (file.getSize() > maxFileSize) {
			throw new IllegalArgumentException("File size exceeds the maximum allowed limit of 5 MB");
		}

		// Validate file type using Apache Tika
		Tika tika = new Tika();
		String detectedType;
		try {
			detectedType = tika.detect(file.getInputStream());
		} catch (IOException e) {
			throw new IllegalArgumentException("Could not detect file type");
		}

		// Allowed MIME types based on mediaType parameter
		List<String> allowedMimeTypes = switch (mediaType) {
			case MediaType.IMAGE -> List.of("image/jpeg", "image/png", "image/gif", "image/svg+xml");
			case MediaType.VIDEO -> List.of("video/mp4", "video/webm", "video/ogg");
			case MediaType.SVG -> List.of("image/svg+xml");
			default -> throw new IllegalArgumentException("Unsupported media type");
		};

		if (!allowedMimeTypes.contains(detectedType)) {
			throw new IllegalArgumentException("Invalid file type: " + detectedType);
		}

		// Optional: further validate SVG content if mediaType == SVG
		if (mediaType == MediaType.SVG) {
			try {
				String svgContent = new String(file.getBytes(), StandardCharsets.UTF_8);
				if (!isValidSvg(svgContent)) {
					throw new IllegalArgumentException("Invalid or unsafe SVG content");
				}
			} catch (IOException e) {
				throw new IllegalArgumentException("Failed to read SVG content");
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