package com.tuttime.restaurant.service.impl;

import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.restaurant.service.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

	private final Path rootLocation = Paths.get("media-uploads");

	@Override
	public String storeFile(MultipartFile file) throws IOException {
		if (file.isEmpty()) {
			throw new InvalidRequestException("Failed to store empty file");
		}

		if (!Files.exists(rootLocation)) {
			Files.createDirectories(rootLocation);
		}

		String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
		Path destinationFile = rootLocation.resolve(filename).normalize().toAbsolutePath();

		Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

		return "/media/" + filename;
	}

	@Override
	public void deleteFile(String fileUrl) {
		if (fileUrl == null || fileUrl.isEmpty()) {
			return;
		}

		try {
			String filename = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
			Path filePath = this.rootLocation.resolve(filename).normalize().toAbsolutePath();
			Files.deleteIfExists(filePath);
		} catch (IOException e) {
			System.err.println("Failed to delete file at " + fileUrl + ": " + e.getMessage());
		}
	}
}
