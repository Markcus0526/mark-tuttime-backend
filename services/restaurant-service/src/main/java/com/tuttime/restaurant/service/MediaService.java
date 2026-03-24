package com.tuttime.restaurant.service;

import com.tuttime.restaurant.entity.EntityType;
import com.tuttime.restaurant.entity.Media;
import com.tuttime.restaurant.exception.ResourceNotFoundException;
import com.tuttime.restaurant.repository.MediaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MediaService {

  private final MediaRepository mediaRepository;

  public MediaService(MediaRepository mediaRepository) {
    this.mediaRepository = mediaRepository;
  }

  public List<Media> getAll() {
    return mediaRepository.findAll();
  }

  public Media getById(Long id) {
    return mediaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Media not found with id " + id));
  }

  public Media create(Media media) {
    if (Boolean.TRUE.equals(media.getIsPrimary())) {
      clearPrimaryForEntity(media.getEntityType(), media.getEntityId());
    }
    return mediaRepository.save(media);
  }

  public Media update(Long id, Media updatedMedia) {
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

  public void delete(Long id) {
    Media existing = getById(id);
    mediaRepository.delete(existing);
  }

  public List<Media> getMediaByEntity(EntityType mediaEntity, Long entityId) {
    return mediaRepository.findByEntityTypeAndEntityId(mediaEntity, entityId);
  }

  private void clearPrimaryForEntity(EntityType entityType, Long entityId) {
    List<Media> primaryMedias = mediaRepository.findByEntityTypeAndEntityIdAndIsPrimaryTrue(entityType, entityId);
    for (Media media : primaryMedias) {
      media.setIsPrimary(false);
      mediaRepository.save(media);
    }
  }
}

