package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OAuthLoginRequest {
	@Schema(description = "OAuth provider name (e.g., GOOGLE, APPLE)", example = "GOOGLE")
	@NotBlank
	String provider;

	@Schema(description = "OAuth access token from provider")
	@NotBlank
	String oauthToken;
}
