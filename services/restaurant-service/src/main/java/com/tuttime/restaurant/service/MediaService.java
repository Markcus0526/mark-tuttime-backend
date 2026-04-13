package com.tuttime.restaurant.service;

import com.tuttime.restaurant.dto.MediaRequest;
import com.tuttime.restaurant.dto.MediaResponse;
import com.tuttime.restaurant.entity.enums.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface MediaService {

	List<MediaResponse> getMediaByEntity(UUID entityId, MediaType mediaType);

	MediaResponse createMedia(MediaRequest request, MultipartFile file, UUID currentUserId, String userRoles);

	MediaResponse updateMedia(UUID mediaId, MediaRequest request, MultipartFile file, UUID currentUserId, String userRoles);

	void deleteMedia(UUID mediaId, UUID currentUserId, String userRoles);

	void deleteAllByEntityId(UUID entityId);

	/**
	 * Retrieves the primary SVG map content for the given entity type and entity ID.
	 *
	 * @param entityId the UUID of the entity
	 * @return SVG content as a String
	 */
	String getMapCanvas(UUID entityId);

	void setOrUpdateMapCanvas(UUID entityId, String svgContent, UUID currentUserId, String userRoles);
}
