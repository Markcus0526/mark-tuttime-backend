package com.tuttime.restaurant.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class FileStorageService {

	private final Path rootLocation = Paths.get("media-uploads");

	public String storeFile(MultipartFile file) throws IOException {
		if (file.isEmpty()) {
			throw new IOException("Failed to store empty file");
		}

		// Create directory if not exists
		if (!Files.exists(rootLocation)) {
			Files.createDirectories(rootLocation);
		}

		// Use a unique filename (e.g., UUID + original filename)
		String filename = java.util.UUID.randomUUID() + "_" + file.getOriginalFilename();
		Path destinationFile = rootLocation.resolve(filename).normalize().toAbsolutePath();

		// Save file
		Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

		// Return URL or path - in production, this would be a public URL or CDN link
		// For local dev, just return a local server URL or file path
		return "/media/" + filename;
	}
}