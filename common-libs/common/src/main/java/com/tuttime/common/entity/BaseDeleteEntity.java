package com.tuttime.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
public abstract class BaseDeleteEntity extends BaseTimeStampEntity {
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