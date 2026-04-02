package com.tuttime.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseUuidEntity implements Persistable<UUID> {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID) // Spring Boot 3.3 / JPA 3.1 feature
	@Column(name = "id", updatable = false, nullable = false)
	private UUID id;

	@Version // <--- THE MAGIC LINE for Optimistic Locking
	private Long version;

	@CreatedDate
	@Column(name = "created_at", updatable = false)
	private OffsetDateTime createdAt;

	@LastModifiedDate
	@Column(name = "updated_at")
	private OffsetDateTime updatedAt;

	@Transient
	private boolean isNew = true;

	@PrePersist
	@PostLoad
	void markNotNew() {
		this.isNew = false;
	}

	@Override
	public boolean isNew() {
		return isNew || id == null;
	}

	public void setId(UUID id) {
		this.id = id;
	}
}