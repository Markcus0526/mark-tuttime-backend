package com.tuttime.booking.entity;

import com.tuttime.common.entity.BaseUuidEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "booking_notifications", schema = "booking_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingNotification extends BaseUuidEntity {

	@Column(name = "booking_id", nullable = false)
	private UUID bookingId;

	@Column(name = "notification_type", nullable = false, length = 50)
	private String notificationType; // SMS, EMAIL, PUSH

	@Column(name = "status", nullable = false, length = 20)
	private String status; // SENT, FAILED, DELIVERED

	@Column(name = "recipient_contact", nullable = false)
	private String recipientContact;

	@Column(name = "sent_at", nullable = false)
	private OffsetDateTime sentAt;
}
