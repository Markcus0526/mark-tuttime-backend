package com.tuttime.booking.dto;

import com.tuttime.booking.entity.enums.WaitlistStatus;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public class WaitlistDto {

    public record WaitlistRequest(
            @NotNull UUID userId,
            @NotNull UUID branchId,
            UUID zoneId,
            @NotNull Integer partySize
    ) {
    }

    public record WaitlistResponse(
            UUID id,
            UUID userId,
            WaitlistStatus status,
            OffsetDateTime joinedAt,
            int position
    ) {
        public static WaitlistResponse fromEntity(com.tuttime.booking.entity.WaitlistEntry entity, int position) {
            return new WaitlistResponse(entity.getId(), entity.getUserId(), entity.getStatus(), entity.getJoinedAt(), position);
        }
    }
}
