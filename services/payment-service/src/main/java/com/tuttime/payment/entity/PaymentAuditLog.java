package com.tuttime.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "payment_audit_logs",
		indexes = {
				@Index(name = "idx_audit_payment_id", columnList = "payment_id"),
				@Index(name = "idx_audit_event_type", columnList = "event_type")
		})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "payment_id", nullable = true)
	private Payment payment;

	@Column(name = "event_type", nullable = false, length = 100)
	private String eventType;

	@Column(name = "event_data", columnDefinition = "jsonb")
	private String eventData;

	@Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
	private OffsetDateTime createdAt;

	// Constructors, getters, setters, equals, hashCode...

	@PrePersist
	public void prePersist() {
		createdAt = OffsetDateTime.now();
	}
}