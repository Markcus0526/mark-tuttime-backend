package com.tuttime.menu.entity;

import com.tuttime.common.entity.BaseUuidEntity;
import com.tuttime.menu.enums.AppliesTo;
import com.tuttime.menu.enums.DiscountType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "discounts")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Discount extends BaseUuidEntity {

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@JdbcType(PostgreSQLEnumJdbcType.class) // Maps Java Enum directly to PostgreSQL Enum
	@Column(name = "discount_type", nullable = false)
	private DiscountType discountType;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@JdbcType(PostgreSQLEnumJdbcType.class)
	@Column(name = "applies_to", nullable = false)
	private AppliesTo appliesTo;

	@Column(name = "reference_id")
	private UUID referenceId; // Can be a Category UUID or an Item UUID

	@Column(name = "start_date")
	private OffsetDateTime startDate;

	@Column(name = "end_date")
	private OffsetDateTime endDate;

	@Column(name = "is_active")
	@Builder.Default
	private Boolean isActive = true;
}