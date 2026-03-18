package com.tuttime.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Block User Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockUserRequest {
    
    @NotBlank(message = "Block reason is required")
    private String reason;
}

