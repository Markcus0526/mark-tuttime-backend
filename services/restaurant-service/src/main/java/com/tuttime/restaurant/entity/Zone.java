package com.tuttime.restaurant.entity;

import com.tuttime.common.entity.BaseUuidAnDeleteEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@jakarta.persistence.Table(name = "zones", schema = "restaurant_service", indexes = {
		@Index(name = "idx_zones_deleted", columnList = "deleted")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Zone extends BaseUuidAnDeleteEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "branch_id", nullable = false)
	private Branch branch;

	@Column(name = "name", nullable = false, length = 255)
	private String name;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Column(name = "capacity", nullable = false)
	private Integer capacity;

	@Builder.Default
	@Column(name = "closed", nullable = false)
	private Boolean closed = false;

}