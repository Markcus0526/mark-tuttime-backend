package com.tuttime.restaurant.service;

import com.tuttime.restaurant.dto.MediaDto;
import com.tuttime.restaurant.entity.enums.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface MediaService {

	List<MediaDto.MediaResponse> getMediaByEntity(UUID entityId, MediaType mediaType);

	MediaDto.MediaResponse createMedia(MediaDto.MediaRequest request, MultipartFile file, UUID currentUserId, String userRoles);

	MediaDto.MediaResponse updateMedia(UUID mediaId, MediaDto.MediaRequest request, MultipartFile file, UUID currentUserId, String userRoles);

	void deleteMedia(UUID mediaId, UUID currentUserId, String userRoles);

	void deleteAllByEntityId(UUID entityId);
}
