package com.tuttime.user.dto;

import com.tuttime.user.enums.AuthProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "DTO for adding an authentication method for a user")
public class AuthMethodCreateRequest {

	@Schema(description = "Authentication provider type", example = "EMAIL_PASS")
	@NotNull(message = "Authentication type is required")
	private AuthProvider authType;

	@Schema(description = "Credential data (hashed password or OAuth ID)")
	@NotBlank(message = "Credential is required")
	private String credential;

}