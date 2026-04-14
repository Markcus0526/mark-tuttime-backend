package com.tuttime.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
public abstract class BaseUuidAnDeleteEntity extends BaseTimeStampEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID) // Spring Boot 3.3 / JPA 3.1 feature
	@Column(name = "id", updatable = false, nullable = false)
	private UUID id;

	@Column(name = "deleted")
	private boolean deleted = false;

	@Column(name = "deleted_at")
	private OffsetDateTime deletedAt;

	@PreUpdate
	public void preUpdate() {
		// Logic: If deleted is true but deletedAt is still null, set it now.
		if (this.deleted && this.deletedAt == null) {
			this.deletedAt = OffsetDateTime.now();
		}
		// Logic: If someone restores the record (Undo delete)
		else if (!this.deleted) {
			this.deletedAt = null;
		}
	}
}