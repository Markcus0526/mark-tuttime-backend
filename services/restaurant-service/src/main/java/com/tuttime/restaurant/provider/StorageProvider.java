package com.tuttime.restaurant.provider;

import org.springframework.web.multipart.MultipartFile;

public interface StorageProvider {

    String store(MultipartFile file);

    String getUrl(String key);

    void delete(String key);
}