package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Block Device Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockDeviceRequest {
    private String reason;
}

