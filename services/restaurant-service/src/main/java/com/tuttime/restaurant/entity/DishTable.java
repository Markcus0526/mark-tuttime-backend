package com.tuttime.restaurant.entity;

import com.tuttime.common.entity.BaseUuidAnDeleteEntity;
import com.tuttime.restaurant.entity.enums.TableStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "tables", schema = "restaurant_service", indexes = {
		@Index(name = "idx_tables_deleted", columnList = "deleted")
}, uniqueConstraints = {
		@UniqueConstraint(name = "unique_zone_table_number", columnNames = {"zone_id", "number"})
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

	@Column(name = "number", nullable = false, length = 50)
	private String number;

	@Column(name = "seats", nullable = false)
	private Integer seats;

	@Builder.Default
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private TableStatus status = TableStatus.ACTIVE;

	@Builder.Default
	@Column(name = "closed", nullable = false)
	private Boolean closed = false;
}