package com.tuttime.booking.entity;

import com.tuttime.common.entity.BaseUuidEntity;
import com.tuttime.common.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
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

    @Column(name = "zone_id")
    private UUID zoneId;

    @ElementCollection
    @CollectionTable(name = "booking_tables", schema = "booking_service",
            joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "table_id")
    private List<UUID> tableIds = new ArrayList<>();

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "booking_status_enum", nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.CREATED;

    @Column(name = "booking_datetime", nullable = false)
    private OffsetDateTime bookingDatetime;

    @Column(name = "reminder_send")
    @Builder.Default
    private Boolean reminderSend = false;

    @Column(name = "duration", nullable = false)
    private Integer duration; // Minutes: Required for the overlap calculation

    @Column(name = "party_size", nullable = false)
    private Integer partySize;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "lead_time_minutes")
    @Builder.Default
    private Integer leadTimeMinutes = 0;

    @Column(name = "grace_period_mins")
    @Builder.Default
    private Integer gracePeriodMins = 15;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "auto_canceled")
    @Builder.Default
    private Boolean autoCanceled = false;

    @Column(name = "guest_name", nullable = false)
    private String guestName;

    @Column(name = "guest_phone", nullable = false)
    private String guestPhone;

    @Column(name = "special_request")
    private String specialRequest;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<BookingItem> items = new ArrayList<>();

    @Version
    private Long version;

    public void addItem(BookingItem item) {
        items.add(item);
        item.setBooking(this);
    }
}
