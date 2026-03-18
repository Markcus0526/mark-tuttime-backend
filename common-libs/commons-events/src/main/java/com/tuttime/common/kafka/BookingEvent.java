package com.tuttime.common.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Kafka event from Apart Sharing platform for booking events.
 * 
 * <p>This event is sent from the Apart Sharing PHP backend when a booking is successful
 * (confirmed & paid). All events from tuttime_booking_success topic are pre-validated
 * and should be processed as confirmed bookings.
 * 
 * <p><b>Event Source:</b> Apart Sharing PHP Backend
 * <p><b>Topic:</b> tuttime_booking_success
 * <p><b>Broker:</b> 81.177.217.115:9092
 * 
 * <p><b>Success Criteria:</b>
 * <ul>
 *   <li>Payment status: success</li>
 *   <li>Order status: active OR completed</li>
 *   <li>Has apartments_booking_id</li>
 *   <li>Not technical order</li>
 *   <li>No fine_id</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingEvent {
    @JsonProperty("order_id")
    private Long orderId;

    @JsonProperty("order_identifier")
    private String orderIdentifier;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("user_phone")
    private String userPhone; // May be null

    @JsonProperty("property_id")
    private Long propertyId;

    @JsonProperty("booking_date")
    private String bookingDate; // ISO Date format (YYYY-MM-DD)

    @JsonProperty("date_time_start")
    private String dateTimeStart; // ISO 8601 datetime format

    @JsonProperty("date_time_end")
    private String dateTimeEnd; // ISO 8601 datetime format

    @JsonProperty("booking_amount")
    private BigDecimal bookingAmount; // Amount in RUB (already converted from kopeks)

    @JsonProperty("guests")
    private Integer guests; // Number of guests (default: 1)

    @JsonProperty("tariff_type")
    private String tariffType; // Tariff type (default: "24h")

    @JsonProperty("timestamp")
    private Long timestamp; // Milliseconds since epoch

    // Legacy fields for backward compatibility (deprecated)
    @JsonProperty("booking_id")
    private Long bookingId; // Deprecated: use order_id instead

    @JsonProperty("confirmed")
    private Boolean confirmed; // Deprecated: all events from tuttime_booking_success are confirmed

    @JsonProperty("non_refundable")
    private Boolean nonRefundable; // Deprecated: not provided by apart-sharing

    @JsonProperty("event_type")
    private String eventType; // Deprecated: all events are "CONFIRMED"

    @JsonProperty("version")
    @Builder.Default
    private String version = "2.0.0"; // Event schema version for evolution support
}

