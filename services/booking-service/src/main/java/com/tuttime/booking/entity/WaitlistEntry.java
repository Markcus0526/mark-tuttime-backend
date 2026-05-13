package com.tuttime.booking.entity;

import com.tuttime.booking.entity.enums.WaitlistStatus;
import com.tuttime.common.entity.BaseUuidEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "waitlist_entries", schema = "booking_service")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class WaitlistEntry extends BaseUuidEntity {

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "branch_id", nullable = false)
	private UUID branchId;

	@Column(name = "zone_id")
	private UUID zoneId;

	@Column(name = "party_size", nullable = false)
	private Integer partySize;

	@Column(name = "joined_at", nullable = false)
	private OffsetDateTime joinedAt;

	@Column(name = "notified_at")
	private OffsetDateTime notifiedAt;

	@Enumerated(EnumType.STRING)
	@JdbcType(PostgreSQLEnumJdbcType.class)
	@Column(name = "status", nullable = false, length = 30)
	@Builder.Default
	private WaitlistStatus status = WaitlistStatus.WAITING;

	@Version
	private Long version;
}
