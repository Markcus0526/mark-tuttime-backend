package com.tuttime.common.event.kafka;

public class TopicConstants {

	private TopicConstants() {}

	// Topics
	public static final String TOPIC_USER_REGISTERED = "tuttime.user.registered";
	public static final String TOPIC_USER_VERIFICATION_REQUESTED = "tuttime.user.verification.requested";
	public static final String TOPIC_USER_PROFILE_UPDATED = "tuttime.user.profile.updated";
	public static final String TOPIC_USER_SECURITY_UPDATED = "tuttime.user.security.updated";

	public static final String TOPIC_RESTAURANT_STAFF_UPDATED = "tuttime.restaurant.staff.updated";

	public static final String TOPIC_BOOKING_STATUS_UPDATED = "tuttime.booking.status.updated";
	public static final String TOPIC_BRANCH_BRANCH_AVAILABILITY = "tuttime.booking.branch.availability";

	public static final String TOPIC_PAYMENT_COMPLETED = "tuttime.payment.completed";
	public static final String TOPIC_PAYMENT_REFUND = "tuttime.payment.refund";

	public static final String TOPIC_ORDER_CREATED = "tuttime.order.created";
	public static final String TOPIC_ORDER_STATUS_UPDATED = "tuttime.order.status.updated";

	public static final String TOPIC_POS_ITEMS_UPDATED = "tuttime.pos.items.updated";
	public static final String TOPIC_POS_PRICE_UPDATED = "tuttime.pos.price.updated";

	public static final String TOPIC_REVIEW_UPDATED = "tuttime.review.updated";
}
