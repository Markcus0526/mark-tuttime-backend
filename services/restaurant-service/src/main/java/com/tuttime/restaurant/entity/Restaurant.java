package com.tuttime.restaurant.entity;

import com.tuttime.common.entity.BaseUuidAnDeleteEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;

@Entity
@Table(name = "restaurants", schema = "restaurant_service", indexes = {
		@Index(name = "idx_restaurants_deleted", columnList = "deleted")
})
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