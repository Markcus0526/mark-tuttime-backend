package com.tuttime.review.entity;

import com.tuttime.common.entity.BaseTimeStampEntity;
import com.tuttime.common.entity.BaseUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "rating_aggregates", schema = "review_service")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RatingAggregate extends BaseTimeStampEntity {
	// Note: In your SQL, branch_id is the Primary Key

	@Id
	@Column(name = "branch_id") // This matches your SQL pkey
	private UUID branchId;

	@Column(name = "average_rating", precision = 3, scale = 2)
	private BigDecimal averageRating;

	@Column(name = "total_reviews")
	private Integer totalReviews;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "rating_dist", columnDefinition = "jsonb")
	private Map<String, Integer> ratingDistribution;
}


