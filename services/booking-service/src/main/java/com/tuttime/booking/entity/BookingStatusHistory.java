package com.tuttime.booking.entity;

import com.tuttime.booking.entity.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "booking_status_histories", schema = "booking_service")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingStatusHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "booking_id", nullable = false)
	private UUID bookingId;

	@Enumerated(EnumType.STRING)
	@JdbcType(PostgreSQLEnumJdbcType.class)
	@Column(name = "old_status", columnDefinition = "booking_status_enum")
	private BookingStatus oldStatus;

	@Enumerated(EnumType.STRING)
	@JdbcType(PostgreSQLEnumJdbcType.class)
	@Column(name = "new_status", columnDefinition = "booking_status_enum", nullable = false)
	private BookingStatus newStatus;

	@Column(name = "changed_by")
	private UUID changedBy;

	@CreatedDate
	@Column(name = "changed_at", nullable = false, updatable = false)
	private Instant changedAt;

	private String reason;
}
