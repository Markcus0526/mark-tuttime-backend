package com.tuttime.admin.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Reorder Tiers Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderTiersRequest {

    @NotEmpty(message = "Tier IDs list is required")
    @NotNull(message = "Tier IDs list cannot be null")
    private List<Long> tierIds; // Ordered list of tier IDs
}

