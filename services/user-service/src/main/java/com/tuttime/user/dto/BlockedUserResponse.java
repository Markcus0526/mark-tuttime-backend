package com.tuttime.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Response DTO for blocked user information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Blocked user information")
public class BlockedUserResponse {
    
    @Schema(description = "User ID", example = "123")
    private Long userId;
    
    @Schema(description = "Username", example = "blocked_user")
    private String username;
    
    @Schema(description = "When the user was blocked")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime blockedAt;
}
