package com.tuttime.restaurant.service.impl;

import java.util.List;
import java.util.UUID;

import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.restaurant.dto.MediaDto;
import com.tuttime.restaurant.enums.EntityType;
import com.tuttime.restaurant.entity.Media;
import com.tuttime.restaurant.mapper.MediaMapper;
import com.tuttime.restaurant.repository.MediaRepository;

import com.tuttime.restaurant.service.MediaService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class MediaServiceImpl implements MediaService {

    private final MediaRepository mediaRepository;
    private final MediaMapper mediaMapper;

    @Override
    public List<Media> getMediaAll() {
        return mediaRepository.findAll();
    }

    @Override
    public Media getMediaById(UUID id) {
        return mediaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with id " + id));
    }

    @Override
    public Media createMedia(MediaDto mediaDto) {
        Media media = mediaMapper.mapToEntity(mediaDto);
        if (Boolean.TRUE.equals(media.getIsPrimary())) {
            clearPrimaryForEntity(media.getEntityType(), media.getEntityId());
        }
        return mediaRepository.save(media);
    }

    @Override
    public Media updateMedia(UUID id, MediaDto updatedMediaDto) {
        Media updatedMedia = mediaMapper.mapToEntity(updatedMediaDto);

        return mediaRepository.findById(id).map(media -> {
            if (Boolean.TRUE.equals(updatedMedia.getIsPrimary()) && !Boolean.TRUE.equals(media.getIsPrimary())) {
                clearPrimaryForEntity(updatedMedia.getEntityType(), updatedMedia.getEntityId());
            }
            media.setUrl(updatedMedia.getUrl());
            media.setAltText(updatedMedia.getAltText());
            media.setIsPrimary(updatedMedia.getIsPrimary());
            media.setMediaType(updatedMedia.getMediaType());
            // entityType and entityId usually won't change, but you can updateMedia if needed
            return mediaRepository.save(media);
        }).orElseThrow(() -> new RuntimeException("Media not found"));
    }

    @Override
    public void deleteMedia(UUID id) {
        Media existing = getMediaById(id);
        mediaRepository.delete(existing);
    }

    @Override
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
