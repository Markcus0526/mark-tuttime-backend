package com.tuttime.user.entity;

import com.tuttime.user.entity.enums.UserStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Data
@Table(name = "users",
		indexes = {
				@Index(name = "idx_users_phone_email", columnList = "phoneNumber, email"),
				@Index(name = "idx_users_is_deleted", columnList = "isDeleted")
		})
public class User {

	@Id
	@GeneratedValue(generator = "UUID")
	@Column(name = "id", updatable = false, nullable = false, length = 36)
	private UUID id;

	@Column(name = "phone_number", length = 20, unique = true)
	private String phoneNumber;

	@Column(name = "email", length = 255, unique = true)
	private String email;

	@Column(name = "full_name", length = 255)
	private String fullName;

	@Column(name = "profile_picture", columnDefinition = "TEXT")
	private String profilePicture;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private UserStatus status = UserStatus.ACTIVE;

	@Column(name = "last_login")
	private OffsetDateTime lastLogin;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt = OffsetDateTime.now();

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt = OffsetDateTime.now();

	@Column(name = "is_blocked", nullable = false)
	private boolean isBlocked = false;

	@Column(name = "is_deleted", nullable = false)
	private boolean isDeleted = false;

	@Column(name = "deleted_at")
	private OffsetDateTime deletedAt;

	// Soft delete check constraint equivalent can be handled in service logic

	// Relationships
	@OneToMany(mappedBy = "userId", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<UserRole> roles = new HashSet<>();

	@OneToMany(mappedBy = "userId", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<UserAuthMethod> authMethods = new HashSet<>();

	@OneToMany(mappedBy = "userId", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<PasswordResetToken> passwordResetTokens = new HashSet<>();

	@OneToMany(mappedBy = "userId", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<UserLocation> locations = new HashSet<>();

	// Constructors, getters, setters

	@PreUpdate
	public void preUpdate() {
		updatedAt = OffsetDateTime.now();
	}

	// Add helper methods to manage bi-directional relationships if needed
	public void setIsDeleted(boolean isDeleted) {
		this.isDeleted = isDeleted;
	}

	public void addAuthMethod(UserAuthMethod method) {
		this.authMethods.add(method);
		method.setUserId(this.getId()); // This is the 'userId' field in UserAuthMethod.java
	}
}