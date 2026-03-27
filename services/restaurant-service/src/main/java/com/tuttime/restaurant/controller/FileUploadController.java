package com.tuttime.restaurant.controller;

import java.io.IOException;
import java.net.URI;

import com.tuttime.restaurant.service.FileStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/uploads")
@Tag(name = "File Upload", description = "Handles media file uploads")
@RequiredArgsConstructor
public class FileUploadController {

	private final FileStorageService fileStorageService;

	@PostMapping("/media")
	@Operation(summary = "Upload a media file and get the accessible URL")
	public ResponseEntity<String> uploadMediaFile(@RequestParam("file") MultipartFile file) throws IOException {
		// Store file and retrieve the accessible URL
		String fileUrl = fileStorageService.storeFile(file);

		// Return the URL in response body or Location header
		return ResponseEntity.created(URI.create(fileUrl)).body(fileUrl);
	}
}