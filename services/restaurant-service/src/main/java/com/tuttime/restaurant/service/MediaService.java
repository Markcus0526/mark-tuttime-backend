package com.tuttime.restaurant.service;

import java.util.List;
import java.util.UUID;

import com.tuttime.restaurant.entity.EntityType;
import com.tuttime.restaurant.entity.Media;
import com.tuttime.restaurant.exception.ResourceNotFoundException;
import com.tuttime.restaurant.repository.MediaRepository;

import org.springframework.stereotype.Service;

@Service
public class MediaService {

	private final MediaRepository mediaRepository;

	public MediaService(MediaRepository mediaRepository) {
		this.mediaRepository = mediaRepository;
	}

	public List<Media> getAll() {
		return mediaRepository.findAll();
	}

	public Media getById(UUID id) {
		return mediaRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Media not found with id " + id));
	}

	public Media create(Media media) {
		if (Boolean.TRUE.equals(media.getIsPrimary())) {
			clearPrimaryForEntity(media.getEntityType(), media.getEntityId());
		}
		return mediaRepository.save(media);
	}

	public Media update(UUID id, Media updatedMedia) {
		return mediaRepository.findById(id).map(media -> {
			if (Boolean.TRUE.equals(updatedMedia.getIsPrimary()) && !Boolean.TRUE.equals(media.getIsPrimary())) {
				clearPrimaryForEntity(updatedMedia.getEntityType(), updatedMedia.getEntityId());
			}
			media.setUrl(updatedMedia.getUrl());
			media.setAltText(updatedMedia.getAltText());
			media.setIsPrimary(updatedMedia.getIsPrimary());
			media.setMediaType(updatedMedia.getMediaType());
			// entityType and entityId usually won't change, but you can update if needed
			return mediaRepository.save(media);
		}).orElseThrow(() -> new RuntimeException("Media not found"));
	}

	public void delete(UUID id) {
		Media existing = getById(id);
		mediaRepository.delete(existing);
	}

	public List<Media> getMediaByEntity(EntityType mediaEntity, UUID entityId) {
		return mediaRepository.findByEntityTypeAndEntityId(mediaEntity, entityId);
	}

	private void clearPrimaryForEntity(EntityType entityType, UUID entityId) {
		List<Media> primaryMedias = mediaRepository.findByEntityTypeAndEntityIdAndIsPrimaryTrue(entityType, entityId);
		for (Media media : primaryMedias) {
			media.setIsPrimary(false);
			mediaRepository.save(media);
		}
	}
}
