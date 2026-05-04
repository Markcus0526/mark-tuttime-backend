package com.tuttime.menu.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.media.storage.type", havingValue = "local", matchIfMissing = true)
@Slf4j
public class LocalStorageProvider implements StorageProvider {

	@Value("${app.media.local.upload-dir}")
	private String uploadDir;
	@Value("${app.media.local.upload-url}")
	private String uploadUrl;
	@Value("${app.media.max-dimension}")
	private int maxDim;
	@Value("${app.media.quality}")
	private float quality;

	@Override
	public String store(MultipartFile file) {
		String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
		Path target = Paths.get(uploadDir).resolve(fileName);

		try {
			Files.createDirectories(target.getParent());
			if (isImage(file.getContentType())) {
				Thumbnails.of(file.getInputStream())
						.size(maxDim, maxDim)
						.outputQuality(quality)
						.toFile(target.toFile());
			} else {
				Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
			}
			return fileName;
		} catch (IOException e) {
			throw new RuntimeException("Failed to store local file", e);
		}
	}

	@Override
	public String getUrl(String key) {
		return uploadUrl + key;
	}

	@Override
	public void delete(String key) {
		try {
			Files.deleteIfExists(Paths.get(uploadDir).resolve(key));
		} catch (IOException e) {
			log.error("Could not delete file: {}", key);
		}
	}

	private boolean isImage(String type) {
		return type != null && type.startsWith("image/");
	}
}

