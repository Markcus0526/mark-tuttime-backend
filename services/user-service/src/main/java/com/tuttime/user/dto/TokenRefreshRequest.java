package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TokenRefreshRequest {
	@Schema(description = "Refresh token to obtain new access token")
	@NotBlank
	String refreshToken;
}