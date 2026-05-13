package com.tuttime.user.entity;

import com.tuttime.common.entity.BaseUuidAnDeleteEntity;
import com.tuttime.common.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", indexes = {
		@Index(name = "idx_users_phone_email", columnList = "phone_number, email")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User extends BaseUuidAnDeleteEntity {

	@Column(name = "phone_number", length = 20, unique = true)
	private String phoneNumber;

	@Column(name = "email", length = 255, unique = true)
	private String email;

	@Column(name = "full_name", length = 255)
	private String fullName;

	@Column(name = "profile_picture", columnDefinition = "TEXT")
	private String profilePicture;

	@Enumerated(EnumType.STRING)
	@JdbcType(PostgreSQLEnumJdbcType.class)
	@Column(columnDefinition = "user_status")
	@Builder.Default
	private UserStatus status = UserStatus.PENDING_VERIFICATION;

	@Column(name = "language", length = 50)
	private String language;

	@Column(name = "last_login")
	private OffsetDateTime lastLogin;

	// Relationships
	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
	@Builder.Default
	private Set<UserRole> roles = new HashSet<>();

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private Set<UserAuthMethod> authMethods = new HashSet<>();

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private Set<UserLocation> locations = new HashSet<>();

	@Version
	private Long version;

	// Helper method to keep bi-directional relationship in sync
	public void addAuthMethod(UserAuthMethod method) {
		this.authMethods.add(method);
		method.setUser(this);
	}
}
