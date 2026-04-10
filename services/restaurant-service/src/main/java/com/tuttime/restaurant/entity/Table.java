package com.tuttime.restaurant.entity;

import com.tuttime.common.entity.BaseDeleteEntity;
import com.tuttime.common.entity.BaseTimeStampEntity;
import com.tuttime.restaurant.entity.enums.TableStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Table extends BaseDeleteEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "table_id", nullable = false, updatable = false)
	private UUID tableId;

	@JoinColumn(name = "zone_id")
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

	public void setIsClosed(boolean isClosed) {
		this.isClosed = isClosed;
	}

	public boolean getIsClosed() {
		return this.isClosed;
	}
}