package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class EmailRequest {
	@Schema(description = "Email address", example = "user@example.com")
	@Email(message = "Invalid email")
	String email;
}
