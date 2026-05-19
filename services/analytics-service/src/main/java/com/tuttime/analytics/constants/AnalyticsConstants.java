package com.tuttime.analytics.constants;

public final class AnalyticsConstants {

    private AnalyticsConstants() {
    } // Prevent instantiation

    // Service Sources
    public static final String SOURCE_BOOKING_SERVICE = "BOOKING_SERVICE";
    public static final String SOURCE_PAYMENT_SERVICE = "PAYMENT_SERVICE";
    public static final String SOURCE_POS_SERVICE = "POS_SERVICE";
    public static final String SOURCE_REVIEW_SERVICE = "REVIEW_SERVICE";

    // Booking Event Types
    public static final String EVENT_BOOKING_CREATED = "BOOKING_CREATED";
    public static final String EVENT_BOOKING_CANCELED = "BOOKING_CANCELED"; // Single 'L' to match Enum
    public static final String EVENT_BOOKING_NO_SHOW = "BOOKING_NO_SHOW";
    public static final String EVENT_BOOKING_COMPLETED = "BOOKING_COMPLETED";

    // Payment Event Types
    public static final String EVENT_PAYMENT_SUCCESS = "PAYMENT_SUCCESS";
    public static final String EVENT_PAYMENT_REFUNDED = "PAYMENT_REFUNDED";

    // Pos Event Types
    public static final String EVENT_POS_APPROVED = "POS_APPROVED";

    // Review Event Types
    public static final String EVENT_REVIEW_APPROVED = "REVIEW_APPROVED";

    // Demand Levels
    public static final String DEMAND_LOW = "LOW";
    public static final String DEMAND_MEDIUM = "MEDIUM";
    public static final String DEMAND_HIGH = "HIGH";
}