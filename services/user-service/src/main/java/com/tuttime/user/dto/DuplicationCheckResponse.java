package com.tuttime.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for duplication check endpoints
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuplicationCheckResponse {
    /**
     * Whether the value (username/email) is already taken/registered
     */
    private boolean isAvailable;
    
    /**
     * The value that was checked
     */
    private String value;
}

