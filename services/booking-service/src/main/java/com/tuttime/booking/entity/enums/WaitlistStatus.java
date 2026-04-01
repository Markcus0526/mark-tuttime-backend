package com.tuttime.booking.entity.enums;

public enum WaitlistStatus {
	WAITING,    // User is in the queue
	NOTIFIED,   // Table became free, user was alerted
	CONVERTED,  // User successfully booked the table
	EXPIRED,    // User didn't respond in time
	CANCELED    // User left the queue
}