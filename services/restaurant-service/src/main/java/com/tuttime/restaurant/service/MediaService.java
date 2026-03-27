package com.tuttime.restaurant.service;

import java.util.List;
import java.util.UUID;

import com.tuttime.restaurant.dto.MediaDto;
import com.tuttime.restaurant.dto.MediaRequest;
import com.tuttime.restaurant.dto.MediaResponse;
import com.tuttime.restaurant.enums.EntityType;
import com.tuttime.restaurant.entity.Media;

public interface MediaService {

	List<MediaResponse> getMediaAll();

	MediaResponse getMediaById(UUID id);

	MediaResponse createMedia(MediaRequest media);

	MediaResponse updateMedia(UUID id, MediaRequest updatedMedia);

	void deleteMedia(UUID id);

	List<MediaResponse> getMediaByEntity(EntityType mediaEntity, UUID entityId);
}
