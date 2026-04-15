package com.tuttime.restaurant.entity;

import com.tuttime.common.entity.BaseUuidAnDeleteEntity;
import com.tuttime.restaurant.entity.enums.TableStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "tables", schema = "restaurant_service", indexes = {
		@Index(name = "idx_tables_deleted", columnList = "deleted")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class DishTable extends BaseUuidAnDeleteEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "zone_id", nullable = false)
	private Zone zone;

	@Column(name = "seats", nullable = false)
	private Integer seats;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM) // Maps to your PostgreSQL 'table_status_enum'
	@Builder.Default
	private TableStatus status = TableStatus.ACTIVE;

	@Column(name = "pos_x", nullable = false)
	private Double posX;

	@Column(name = "pos_y", nullable = false)
	private Double posY;

	@Column(name = "table_type", length = 20)
	private String tableType; // e.g., 'small', 'medium', 'large'

	@Column(name = "label", nullable = false, length = 50)
	private String label;

	@Column(name = "notes")
	private String notes;

	@Builder.Default
	@Column(name = "closed", nullable = false)
	private Boolean closed = false;
}