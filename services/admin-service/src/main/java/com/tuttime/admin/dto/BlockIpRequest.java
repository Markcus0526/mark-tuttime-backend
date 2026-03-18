package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Block IP Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockIpRequest {
    private String reason;
}

