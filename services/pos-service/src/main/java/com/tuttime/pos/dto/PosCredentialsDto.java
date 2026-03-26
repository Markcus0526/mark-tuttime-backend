package com.tuttime.pos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PosCredentialsDto {
	@NotNull(message = "Restaurant ID must not be null")
	private Long restaurantId;

	@NotBlank(message = "POS system must not be blank")
	private String posSystem;

	@NotBlank(message = "API URL must not be blank")
	private String apiUrl;

	@NotBlank(message = "API key must not be blank")
	private String apiKey;

	@NotBlank(message = "API token must not be blank")
	private String apiToken;
}