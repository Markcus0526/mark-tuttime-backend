package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "DTO for assigning a role to a user")
public class RoleAssignRequest {

	@Schema(description = "Role name to assign or remove", example = "ADMIN")
	@NotBlank
	String roleName;
}