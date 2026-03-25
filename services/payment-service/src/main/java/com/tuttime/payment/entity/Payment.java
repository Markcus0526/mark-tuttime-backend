package com.tuttime.payment.entity;

import com.tuttime.payment.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;

@Entity
@Table(name = "payments",
		indexes = {
				@Index(name = "idx_payment_booking_id", columnList = "bookingId"),
				@Index(name = "idx_payment_status", columnList = "status")
		})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "payment_intent_id", nullable = false, unique = true, length = 255)
	private String paymentIntentId;

	@Column(name = "booking_id")
	private Long bookingId; // Assuming bookingId is a Long; change if different or map as @ManyToOne

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false, length = 10)
	private String currency;

	@Column(name = "payment_method", nullable = false, length = 50)
	private String paymentMethod;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PaymentStatus status; // Could be an Enum with @Enumerated

	@Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
	private OffsetDateTime updatedAt;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(name = "receipt_url", length = 500)
	private String receiptUrl;

	@OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<Refund> refunds;

	@OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<PaymentAuditLog> auditLogs;

	// Constructors, getters, setters, equals, hashCode...

	@PrePersist
	public void prePersist() {
		OffsetDateTime now = OffsetDateTime.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	public void preUpdate() {
		updatedAt = OffsetDateTime.now();
	}

}
