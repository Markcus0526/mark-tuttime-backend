package com.tuttime.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users", schema = "user_service",
		indexes = @Index(name = "idx_users_phone_email", columnList = "phone_number,email"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
	@Id
	@GeneratedValue(generator = "gen_random_uuid")
	private UUID id;

	@Column(name = "phone_number", unique = true)
	private String phoneNumber;

	@Column(name = "email", unique = true)
	private String email;

	@Column(name = "full_name")
	private String fullName;

	@Column(name = "profile_picture", columnDefinition = "TEXT")
	private String profilePicture;

	@Builder.Default
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private UserStatus status = UserStatus.ACTIVE;

	@Column(name = "last_login")
	private Instant lastLogin;

	@Builder.Default
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	@Builder.Default
	@Column(name = "updated_at")
	private Instant updatedAt = Instant.now();

	@Builder.Default
	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<UserRole> roles = new HashSet<>();

	@Builder.Default
	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<UserAuthMethod> authMethods = new HashSet<>();

	@Builder.Default
	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<UserLocation> locations = new HashSet<>();
}