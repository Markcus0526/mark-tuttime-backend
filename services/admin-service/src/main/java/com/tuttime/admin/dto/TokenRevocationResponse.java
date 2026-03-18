package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token Revocation Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRevocationResponse {
    private Long userId;
    private Integer revokedCount;
}

