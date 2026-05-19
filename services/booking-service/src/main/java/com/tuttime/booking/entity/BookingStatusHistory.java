package com.tuttime.booking.entity;

import com.tuttime.common.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "booking_status_histories", schema = "booking_service")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class BookingStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, updatable = false)
    private Booking booking;

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
    private OffsetDateTime changedAt;

    private String reason;

    public static BookingStatusHistory of(Booking booking, BookingStatus oldStatus, UUID changedBy, String reason) {
        return BookingStatusHistory.builder()
                .booking(booking)
                .oldStatus(oldStatus)
                .newStatus(booking.getStatus())
                .changedBy(changedBy)
                .reason(reason)
                .build();
    }
}
