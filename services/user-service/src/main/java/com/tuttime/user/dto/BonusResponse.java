package com.tuttime.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;

/**
 * Response DTO for adding bonus
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BonusResponse {
    private Long bonusId;
    private String bonusType;
    private BigDecimal amount;
    private BigDecimal newBalance;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime addedAt;
}

