package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pre-signed URL response for S3 file access")
public class PresignedUrlResponse {
    
    @Schema(description = "S3 file key/path", example = "jsons/cities/CC.json")
    private String fileKey;
    
    @Schema(description = "Pre-signed URL for accessing the file", 
            example = "https://s3.ru-7.storage.selcloud.ru/tuttime-storage/jsons/cities/CC.json?X-Amz-Algorithm=...")
    private String presignedUrl;
    
    @Schema(description = "Expiration time in hours", example = "24")
    private Long expirationHours;
}
