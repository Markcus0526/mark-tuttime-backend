package com.tuttime.restaurant.service;

import com.tuttime.restaurant.dto.MediaRequest;
import com.tuttime.restaurant.dto.MediaResponse;
import com.tuttime.restaurant.enums.EntityType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface MediaService {

	List<MediaResponse> getMediaByEntity(EntityType entityType, UUID entityId);

	MediaResponse createMedia(MediaRequest request, MultipartFile file);

	MediaResponse updateMedia(UUID mediaId, MediaRequest request);

	void deleteMedia(UUID mediaId);

	/**
	 * Retrieves the primary SVG map content for the given entity type and entity ID.
	 *
	 * @param entityType the type of entity (e.g., BRANCH)
	 * @param entityId   the UUID of the entity
	 * @return SVG content as a String
	 */
	String getPrimarySvgMap(EntityType entityType, UUID entityId);

	void setOrUpdateSvgMap(EntityType entityType, UUID entityId, String svgContent);
}
