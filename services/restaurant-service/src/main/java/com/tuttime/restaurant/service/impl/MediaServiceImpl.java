package com.tuttime.restaurant.service.impl;

import com.tuttime.restaurant.dto.MediaRequest;
import com.tuttime.restaurant.dto.MediaResponse;
import com.tuttime.restaurant.entity.Media;
import com.tuttime.restaurant.enums.EntityType;
import com.tuttime.restaurant.enums.MediaType;
import com.tuttime.restaurant.mapper.MediaMapper;
import com.tuttime.restaurant.repository.MediaRepository;
import com.tuttime.restaurant.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
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

    @Override
    public List<MediaResponse> getMediaByEntity(EntityType entityType, UUID entityId) {
        List<Media> medias = mediaRepository.findByEntityTypeAndEntityId(entityType, entityId);
        return medias.stream()
                .map(mediaMapper::toResponse)
                .collect(Collectors.toList());
    }

//    @Override
//    @Transactional
//    public MediaResponse createMedia(MediaRequest request) {
//        Media media = mediaMapper.toEntity(request);
//        Media saved = mediaRepository.save(media);
//        return mediaMapper.toResponse(saved);
//    }
    @Override
    public MediaResponse createMedia(MediaRequest request, MultipartFile file) {
        // Validate file, store it (filesystem, cloud, DB), then save media metadata
        String storedFileUrl = fileStorageService.storeFile(file);

        Media media = new Media();
        media.setEntityType(request.getEntityType());
        media.setEntityId(request.getEntityId());
        media.setDescription(request.getDescription());
        media.setUrl(storedFileUrl);
        mediaRepository.save(media);

        return modelMapper.map(media, MediaResponse.class);
    }

    @Override
    @Transactional
    public MediaResponse updateMedia(UUID mediaId, MediaRequest request) {
        Media existing = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new IllegalArgumentException("Media not found: " + mediaId));

        mediaMapper.updateEntityFromRequest(request, existing);

        Media updated = mediaRepository.save(existing);
        return mediaMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteMedia(UUID mediaId) {
        if (!mediaRepository.existsById(mediaId)) {
            throw new IllegalArgumentException("Media not found: " + mediaId);
        }
        mediaRepository.deleteById(mediaId);
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
    public void setOrUpdateSvgMap(EntityType entityType, UUID entityId, String svgContent) {
        Optional<Media> existingMapOpt = mediaRepository.findPrimaryMapByEntity(entityType, entityId);

        Media media;
        if (existingMapOpt.isPresent()) {
            media = existingMapOpt.get();
            media.setSvgContent(svgContent);
            media.setUpdatedAt(Instant.now());
        } else {
            media = Media.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .mediaType(MediaType.SVG) // SVG media type
                    .svgContent(svgContent)
                    .isMap(true)
                    .isPrimary(true) // Mark as primary map if needed
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .isDeleted(false)
                    .build();
        }
        mediaRepository.save(media);
    }
}