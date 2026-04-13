package com.tuttime.restaurant.entity;

import com.tuttime.common.entity.BaseDeleteEntity;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@jakarta.persistence.Table(name = "branches", schema = "restaurant_service", indexes = {
		@Index(name = "idx_branches_deleted", columnList = "deleted")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Branch extends BaseDeleteEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "branch_id", nullable = false, updatable = false)
	private UUID branchId;

	@JoinColumn(name = "restaurant_id")
	private UUID restaurantId;

	@Column(name = "name", nullable = false, length = 255)
	private String name;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Column(name = "address", nullable = false, length = 512)
	private String address;

	@Column(name = "phone", length = 20)
	private String phone;

	@Column(name = "timezone", nullable = false, length = 50)
	private String timezone;

	@Column(name = "location", columnDefinition = "geography(Point,4326)")
	private Point location;

	@Column(name = "rating", precision = 2, scale = 1)
	private BigDecimal rating;

	@PrePersist
	public void prePersist() {
		if (rating == null) {
			rating = BigDecimal.ZERO;
		}
	}
}
