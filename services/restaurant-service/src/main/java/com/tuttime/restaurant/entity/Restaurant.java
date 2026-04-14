package com.tuttime.restaurant.entity;

import com.tuttime.common.entity.BaseUuidAnDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.PrePersist;
import lombok.*;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;

@Entity
@jakarta.persistence.Table(name = "restaurants")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Restaurant extends BaseUuidAnDeleteEntity {

	@Column(name = "name", nullable = false, length = 255)
	private String name;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Column(name = "address", nullable = false, length = 512)
	private String address;

	@Column(name = "location", columnDefinition = "geography(Point,4326)")
	private Point location;

	@Column(name = "phone", length = 20)
	private String phone;

	@Column(name = "website", length = 255)
	private String website;

	@Column(name = "rating", precision = 2, scale = 1)
	private BigDecimal rating;

	@PrePersist
	public void prePersist() {
		if (rating == null) {
			rating = BigDecimal.ZERO;
		}
	}
}