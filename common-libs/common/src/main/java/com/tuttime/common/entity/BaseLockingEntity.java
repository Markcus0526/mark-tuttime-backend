package com.tuttime.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseLockingEntity extends BaseTimeStampEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Version // <--- THE MAGIC LINE for Optimistic Locking
	private Long version;

	@Column(name = "deleted_at")
	private OffsetDateTime deletedAt;

	// Helper for soft delete
	public boolean isDeleted() {
		return deletedAt != null;
	}
}