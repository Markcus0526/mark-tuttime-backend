package com.tuttime.restaurant.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface FileStorageService {
	String storeFile(MultipartFile file) throws IOException;
	void deleteFile(String fileUrl);
}
