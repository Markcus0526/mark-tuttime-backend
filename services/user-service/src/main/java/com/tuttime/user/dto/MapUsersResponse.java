package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for map users query
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing users for map display")
public class MapUsersResponse {
    
    @Schema(description = "List of users on map")
    private List<MapUserResponse> users;

    @Schema(description = "Total number of users matching query", example = "150")
    private Long total;

    @Schema(description = "Maximum number of results returned", example = "100")
    private Integer limit;

    @Schema(description = "Pagination offset", example = "0")
    private Integer offset;
}

