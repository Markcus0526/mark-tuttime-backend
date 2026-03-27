package com.tuttime.restaurant.service;

import java.util.List;
import java.util.UUID;

import com.tuttime.restaurant.entity.EntityType;
import com.tuttime.restaurant.entity.Media;

public interface MediaService {

	List<Media> getAll();

	Media getById(UUID id);

	Media create(Media media);

	Media update(UUID id, Media updatedMedia);

	void delete(UUID id);

	List<Media> getMediaByEntity(EntityType mediaEntity, UUID entityId);
}
