package com.tuttime.user.entity;

import com.tuttime.common.entity.BaseTimeStampEntity;
import com.tuttime.user.entity.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
		@Index(name = "idx_users_phone_email", columnList = "phone_number, email")
})
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User extends BaseTimeStampEntity {

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
	@Builder.Default
	private UserStatus status = UserStatus.ACTIVE;

	@Column(name = "last_login")
	private OffsetDateTime lastLogin;

	// Relationships
	@OneToMany(mappedBy = "userId", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private Set<UserRole> roles = new HashSet<>();

	@OneToMany(mappedBy = "userId", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private Set<UserAuthMethod> authMethods = new HashSet<>();

	@OneToMany(mappedBy = "userId", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private Set<UserLocation> locations = new HashSet<>();

	// Helper method to keep bi-directional relationship in sync
	public void addAuthMethod(UserAuthMethod method) {
		this.authMethods.add(method);
		method.setUserId(this.getId());
	}
}
