package com.tuttime.user.entity;

import com.tuttime.user.enums.AuthProvider;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "user_auth_methods", schema = "user_service",
		uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "auth_type"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAuthMethod {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(name = "auth_type", nullable = false)
	private AuthProvider authType;

	@Column(columnDefinition = "TEXT")
	private String credential; // hashed password or OAuth ID

	@Builder.Default
	@Column(nullable = false)
	private boolean verified = false;

	@Builder.Default
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt = Instant.now();
}
