package com.tuttime.common.enums;

public enum NotificationTemplateKey {
	// ACCOUNT
	USER_REGISTER,              // For email/SMS onboarding.
	USER_VERIFICATION,          // For email/SMS onboarding.
	USER_LOGIN,                 // Security alert sent when a new login occurs from an unknown device
	PASSWORD_RESET,             // Essential for recovery.
	MFA_CODE,                   // For high-security logins or sensitive manager actions.
	ACCOUNT_LOCKED,             // Security alert when too many failed login attempts occur.
	ADMIN_USER_CREATED,         // Internal staff onboarding.
	ADMIN_USER_BLOCKED,         // Internal staff onboarding.
	ADMIN_USER_UNBLOCKED,       // Internal staff onboarding.
	ADMIN_USER_DELETED,         // Internal staff onboarding.

	// BOOKING (Customer)
	BOOKING_CREATED,            // Confirmation.
	BOOKING_REMINDER,           // Sent 2–24 hours before the booking. Crucial for reducing no-shows.
	BOOKING_CANCELED,           // Cancellation confirmation.
	BOOKING_COMPLETED,          // Summary after the meal.

	// BOOKING (Manager Fan-out)
	MANAGER_BOOKING_ALERT,      // For new incoming bookings.
	MANAGER_BOOKING_CANCEL,     // Urgent! Tells staff to free a table immediately.
	MANAGER_LOW_AVAILABILITY,   // Alert when a branch is nearly 100% booked.

	// PAYMENTS
	PAYMENT_SUCCESS,            // Receipt.
	PAYMENT_FAILED,             // Crucial so the user can retry before the booking is timed out.
	PAYMENT_REFUNDED,           // Confirmation of money back.

	// REVIEWS
	REVIEW_REQUEST,             //  Sent 1 hour after COMPLETED to trigger growth.
	REVIEW_APPROVED,            //  Feedback that their comment is live.
	REVIEW_REJECTED             // Explanation of why a review was hidden (e.g., profanity).

}