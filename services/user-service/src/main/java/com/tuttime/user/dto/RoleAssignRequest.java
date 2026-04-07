package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "DTO for assigning a role to a user")
public class RoleAssignRequest {

	@Schema(description = "Role name to assign or remove", example = "ADMIN")
	@NotBlank
	String roleName;
}