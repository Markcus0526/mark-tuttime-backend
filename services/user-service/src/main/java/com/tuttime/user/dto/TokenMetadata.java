package com.tuttime.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Token metadata stored in Redis whitelist
 * 
 * <p>This DTO represents the metadata associated with a whitelisted JWT token,
 * including user information, token type, and expiration details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenMetadata implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** User ID associated with the token */
    @JsonProperty("userId")
    private Long userId;
    
    /** Token type: "access" or "refresh" */
    @JsonProperty("tokenType")
    private String tokenType;
    
    /** Token issued timestamp (milliseconds since epoch) */
    @JsonProperty("issuedAt")
    private Long issuedAt;
    
    /** Token expiration timestamp (milliseconds since epoch) */
    @JsonProperty("expiresAt")
    private Long expiresAt;
    
    /** Device ID associated with the token (optional) */
    @JsonProperty("deviceId")
    private String deviceId;
}

