package com.tuttime.booking.entity;

import com.tuttime.booking.entity.enums.BookingStatus;
import com.tuttime.common.entity.BaseUuidEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings", schema = "booking_service")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Booking extends BaseUuidEntity {

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "restaurant_id", nullable = false)
	private UUID restaurantId;

	@Column(name = "branch_id", nullable = false)
	private UUID branchId;

	@Column(name = "zone_id", nullable = false)
	private UUID zoneId;

	@Column(name = "table_id", nullable = false)
	private UUID tableId;

	@Enumerated(EnumType.STRING)
	@JdbcType(PostgreSQLEnumJdbcType.class)
	@Column(columnDefinition = "booking_status_enum", nullable = false)
	@Builder.Default
	private BookingStatus status = BookingStatus.CREATED;

	@Column(name = "booking_datetime", nullable = false)
	private OffsetDateTime bookingDatetime;

	@Column(name = "duration", nullable = false)
	private Integer duration; // Minutes: Required for the overlap calculation

	@Column(name = "party_size", nullable = false)
	private Integer partySize;

	@Column(name = "total_amount", nullable = false)
	private BigDecimal totalAmount;

	@Column(name = "lead_time_minutes")
	@Builder.Default
	private Integer leadTimeMinutes = 0;

	@Column(name = "grace_period_mins")
	@Builder.Default
	private Integer gracePeriodMins = 15;

	@Column(name = "cancellation_reason")
	private String cancellationReason;

	@Column(name = "auto_cancelled")
	@Builder.Default
	private Boolean autoCancelled = false;
}
