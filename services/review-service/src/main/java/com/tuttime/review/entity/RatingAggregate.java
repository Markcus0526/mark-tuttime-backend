package com.tuttime.review.entity;

import com.tuttime.common.entity.BaseTimeStampEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "rating_aggregates", schema = "review_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingAggregate extends BaseTimeStampEntity {

	@Id
	@Column(name = "restaurant_id")
	private UUID restaurantId;

	@Column(precision = 3, scale = 2)
	@Builder.Default
	private BigDecimal averageRating = BigDecimal.ZERO;

	@Column(name = "total_reviews")
	@Builder.Default
	private Integer totalReviews = 0;

	/**
	 * Perfect JSONB Mapping: Hibernate 6.5 (Boot 3.3.x default)
	 * automatically converts this Map to/from a PostgreSQL JSONB column.
	 */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "rating_dist", columnDefinition = "jsonb")
	@Builder.Default
	private Map<String, Integer> ratingDistribution = new HashMap<>();
}

