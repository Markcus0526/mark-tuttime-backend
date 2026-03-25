package com.tuttime.user.dto;

import com.tuttime.user.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
	private UUID id;
	private String phoneNumber;
	private String email;
	private String fullName;
	private String profilePicture;
	private UserStatus status;
	private Instant lastLogin;
	private Instant createdAt;
	private Instant updatedAt;
	private Set<String> roles; // Role names
}