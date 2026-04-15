package com.tuttime.menu.service.impl;

import com.tuttime.menu.dto.MediaDto;
import com.tuttime.menu.entity.Media;
import com.tuttime.menu.entity.enums.MediaType;
import com.tuttime.menu.mapper.MediaMapper;
import com.tuttime.menu.repository.MediaRepository;
import com.tuttime.menu.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

	private final MediaRepository mediaRepository;
	private final MediaMapper mediaMapper;

	@Value("${app.image.local.upload-dir}")
	private String uploadDir;

	@Override
	@Transactional(readOnly = true)
	public List<MediaDto.MediaResponse> getMediaByEntity(UUID entityId, MediaType mediaType) {
		// If mediaType is null, the query returns all media for the entity
		return mediaRepository.findAllByEntityIdAndOptionalMediaType(entityId, mediaType)
				.stream()
				.map(mediaMapper::toResponse)
				.toList(); // Simplified for Java 16+
	}

	@Override
	@Transactional
	public MediaDto.MediaResponse createMedia(MediaDto.MediaRequest request, MultipartFile file, UUID userId, String roles) {
		// 1. Save physical file
		String fileName = saveFile(file);

		// 2. Map metadata
		Media media = Media.builder()
				.entityId(request.entityId())
				.url("/menu-medias/" + fileName) // Virtual path served by WebMvcConfig
				.mediaType(detectMediaType(file))
				.primaryEntity(request.primaryEntity())
				.altText(request.altText())
				.build();

		return mediaMapper.toResponse(mediaRepository.save(media));
	}

	@Override
	@Transactional
	public MediaDto.MediaResponse updateMedia(UUID mediaId, MediaDto.MediaRequest request, MultipartFile file, UUID userId, String roles) {
		Media existing = mediaRepository.findById(mediaId)
				.orElseThrow(() -> new RuntimeException("Media not found"));

		// 1. Handle File Replacement if a new file is provided
		if (file != null && !file.isEmpty()) {
			deletePhysicalFile(existing.getUrl()); // Delete old file from VPS
			String newFileName = saveFile(file);   // Save new file
			existing.setUrl("/menu-medias/" + newFileName);
			existing.setMediaType(detectMediaType(file));
		}

		// 2. Update Metadata
		existing.setAltText(request.altText());
		existing.setPrimaryEntity(request.primaryEntity());

		return mediaMapper.toResponse(mediaRepository.save(existing));
	}

	@Override
	@Transactional
	public void deleteMedia(UUID mediaId, UUID userId, String roles) {
		Media media = mediaRepository.findById(mediaId)
				.orElseThrow(() -> new RuntimeException("Media not found"));

		deletePhysicalFile(media.getUrl()); // Physical cleanup
		mediaRepository.delete(media);      // Database cleanup
	}

	private String saveFile(MultipartFile file) {
		String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
		try {
			Path targetPath = Paths.get(uploadDir).resolve(fileName);
			Files.createDirectories(targetPath.getParent());
			Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
			return fileName;
		} catch (IOException e) {
			throw new RuntimeException("Could not store file on VPS", e);
		}
	}

	private void deletePhysicalFile(String virtualPath) {
		// Converts "/media/filename.jpg" -> "/var/www/menu-media/filename.jpg"
		String fileName = virtualPath.replace("/menu-medias/", "");
		try {
			Files.deleteIfExists(Paths.get(uploadDir).resolve(fileName));
		} catch (IOException e) {
			// Log warning but don't fail transaction if file is already gone
		}
	}

	private MediaType detectMediaType(MultipartFile file) {
		String contentType = file.getContentType();
		if (contentType != null && contentType.startsWith("video")) {
			return MediaType.VIDEO;
		}
		return MediaType.IMAGE;
	}
}