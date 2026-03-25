package com.tuttime.menu.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "discounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Discount {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 255)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "discount_type", nullable = false)
	private DiscountTypeEnum discountType;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(name = "applies_to", nullable = false)
	private AppliesToEnum appliesTo;

	@Column(name = "reference_id")
	private Long referenceId;

	@Column(name = "start_date")
	private OffsetDateTime startDate;

	@Column(name = "end_date")
	private OffsetDateTime endDate;

	@Column(name = "is_active")
	private Boolean isActive = true;

	@Column(name = "created_at", updatable = false)
	@CreationTimestamp
	private OffsetDateTime createdAt;

	public enum DiscountTypeEnum {
		PERCENTAGE, FIXED_AMOUNT
	}

	public enum AppliesToEnum {
		ITEM, CATEGORY, BRANCH
	}
}