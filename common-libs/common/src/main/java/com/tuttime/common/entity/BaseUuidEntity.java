package com.tuttime.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseUuidEntity extends BaseTimeStampEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID) // Spring Boot 3.3 / JPA 3.1 feature
	@Column(name = "id", updatable = false, nullable = false)
	private UUID id;

	@Version // <--- THE MAGIC LINE for Optimistic Locking
	private Long version;

	@Transient
	private boolean isNew = true;

	@PrePersist
	@PostLoad
	void markNotNew() {
		this.isNew = false;
	}

	public boolean isNew() {
		return isNew || id == null;
	}
}