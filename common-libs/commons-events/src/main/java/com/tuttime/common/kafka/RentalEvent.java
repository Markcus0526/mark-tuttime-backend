package com.tuttime.common.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka event from Apart Sharing platform for rental listing events.
 * 
 * <p>This event is sent from the Apart Sharing PHP backend when an apartment
 * is successfully listed (synced & available). All events from tuttime_rental_success
 * topic are pre-validated and should be processed as confirmed rentals.
 * 
 * <p><b>Event Source:</b> Apart Sharing PHP Backend
 * <p><b>Topic:</b> tuttime_rental_success
 * <p><b>Broker:</b> 81.177.217.115:9092
 * 
 * <p><b>Success Criteria:</b>
 * <ul>
 *   <li>Not archived: is_archive = false</li>
 *   <li>Available for search: is_available_for_search = true</li>
 *   <li>Synced: bnovo_sync = true OR is_realty_sync = true</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalEvent {
    @JsonProperty("property_id")
    private Long propertyId;

    @JsonProperty("user_id")
    private Long userId; // Apartment owner

    @JsonProperty("user_phone")
    private String userPhone; // May be null

    @JsonProperty("listing_date")
    private String listingDate; // ISO Date format (YYYY-MM-DD)

    @JsonProperty("city_id")
    private Integer cityId;

    @JsonProperty("apartment_title")
    private String apartmentTitle;

    @JsonProperty("is_bnovo_sync")
    private Boolean isBnovoSync;

    @JsonProperty("is_realty_sync")
    private Boolean isRealtySync;

    @JsonProperty("is_available_for_search")
    private Boolean isAvailableForSearch;

    @JsonProperty("timestamp")
    private Long timestamp; // Milliseconds since epoch

    // Legacy fields for backward compatibility (deprecated)
    @JsonProperty("rental_id")
    private Long rentalId; // Deprecated: not provided by apart-sharing

    @JsonProperty("rental_date")
    private String rentalDate; // Deprecated: use listing_date instead

    @JsonProperty("confirmed")
    private Boolean confirmed; // Deprecated: all events from tuttime_rental_success are confirmed

    @JsonProperty("non_refundable")
    private Boolean nonRefundable; // Deprecated: not provided by apart-sharing

    @JsonProperty("rental_amount")
    private java.math.BigDecimal rentalAmount; // Deprecated: not provided by apart-sharing

    @JsonProperty("event_type")
    private String eventType; // Deprecated: all events are "CONFIRMED"

    @JsonProperty("version")
    @Builder.Default
    private String version = "2.0.0"; // Event schema version for evolution support
}

