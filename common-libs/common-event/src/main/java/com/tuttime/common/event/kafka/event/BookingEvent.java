package com.tuttime.common.event.kafka.event;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.tuttime.common.enums.BookingStatus;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class BookingEvent {

    @Builder(toBuilder = true)
    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record BookingStatusUpdatedEvent(
            UUID eventId,
            UUID bookingId,
            List<UUID> tableIds,
            UUID branchId,
            UUID zoneId,
            UUID userId,
            BookingStatus oldStatus,
            BookingStatus newStatus,
            OffsetDateTime bookingTime,
            UUID changedBy,
            String reason,
            Integer partySize,
            OffsetDateTime occurredAt
    ) implements BaseEvent {

        // Canonical Constructor to handle default values manually
        public BookingStatusUpdatedEvent {
            if (eventId == null) eventId = UUID.randomUUID();
            if (occurredAt == null) occurredAt = OffsetDateTime.now();
        }

        @Override
        public UUID eventId() {
            return eventId;
        }

        @Override
        public OffsetDateTime occurredAt() {
            return occurredAt;
        }
    }

    @Builder(toBuilder = true)
    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record BranchAvailabilityEvent(
            UUID eventId,
            String branchId,
            String branchName,       // For friendly notification text
            double occupancyPercent,  // e.g., 95.0
            int totalTables,
            int bookedTables,
            OffsetDateTime occurredAt
    ) implements BaseEvent {

        // Canonical Constructor to handle default values manually
        public BranchAvailabilityEvent {
            if (eventId == null) eventId = UUID.randomUUID();
            if (occurredAt == null) occurredAt = OffsetDateTime.now();
        }

        @Override
        public UUID eventId() {
            return eventId;
        }

        @Override
        public OffsetDateTime occurredAt() {
            return occurredAt;
        }
    }
}
