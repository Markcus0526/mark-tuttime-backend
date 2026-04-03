package com.tuttime.restaurant.entity;

import com.tuttime.restaurant.enums.TableStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@jakarta.persistence.Table(name = "tables", schema = "restaurant_service", indexes = {
		@Index(name = "idx_tables_is_deleted", columnList = "is_deleted")
}, uniqueConstraints = {
		@UniqueConstraint(name = "unique_zone_table_number", columnNames = {"zone_id", "number"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Table {

	@Id
	@GeneratedValue
	@Column(name = "table_id", nullable = false, updatable = false)
	private UUID tableId;

	@Column(name = "zone_id", nullable = false)
	private UUID zoneId;

	@Column(name = "number", nullable = false, length = 50)
	private String number;

	@Column(name = "seats", nullable = false)
	private Integer seats;

	@Builder.Default
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private TableStatus status = TableStatus.ACTIVE;

	@Builder.Default
	@Column(name = "is_closed", nullable = false)
	private boolean isClosed = false;

	@Builder.Default
	@Column(name = "is_deleted", nullable = false)
	private boolean isDeleted = false;

	@Column(name = "deleted_at")
	private OffsetDateTime deletedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at")
	private OffsetDateTime updatedAt;

	@PrePersist
	public void prePersist() {
		updatedAt = createdAt;
	}

	@PreUpdate
	public void preUpdate() {
		updatedAt = Instant.now().atOffset(ZoneOffset.UTC);
	}

	public void setIsClosed(boolean isClosed) {
		this.isClosed = isClosed;
	}

	public boolean getIsClosed() {
		return this.isClosed;
	}

	public void setIsDeleted(boolean isDeleted) {
		this.isDeleted = isDeleted;
	}

	public boolean getIsDeleted() {
		return this.isDeleted;
	}
}