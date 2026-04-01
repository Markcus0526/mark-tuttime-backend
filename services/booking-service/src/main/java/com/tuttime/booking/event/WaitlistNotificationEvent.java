package com.tuttime.booking.event;

import java.util.UUID;

public record WaitlistNotificationEvent(
		UUID userId,
		UUID branchId,
		UUID waitlistId,
		String message
) {}
