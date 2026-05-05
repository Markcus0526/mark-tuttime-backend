package com.tuttime.restaurant.entity;

import com.tuttime.common.entity.BaseUuidAnDeleteEntity;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "branches", schema = "restaurant_service", indexes = {
		@Index(name = "idx_branches_deleted", columnList = "deleted")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Branch extends BaseUuidAnDeleteEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "restaurant_id", nullable = false)
	private Restaurant restaurant;

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

	@Builder.Default
	@Column(name = "closed", nullable = false)
	private Boolean closed = false;

	@Column(name = "min_price", precision = 12, scale = 2)
	private BigDecimal minPrice;

	@Column(name = "max_price", precision = 12, scale = 2)
	private BigDecimal maxPrice;

	@OneToMany(mappedBy = "branch", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private Set<BranchTag> branchTags = new HashSet<>();

	@PrePersist
	public void prePersist() {
		if (rating == null) {
			rating = BigDecimal.ZERO;
		}
	}
}
