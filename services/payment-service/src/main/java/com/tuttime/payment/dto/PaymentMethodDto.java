package com.tuttime.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

public class PaymentMethodDto {

	/**
	 * Used for Method 2: Manually adding a card in Profile
	 */
	@Builder
	public record BindingRequest(
			@Schema(description = "Return URL after 3D-Secure check", example = "https://eatnow.app")
			String returnUrl,

			@Schema(description = "Set as primary card after binding")
			boolean setAsDefault
	) {
	}

	/**
	 * Response for both Profile & Select Payment pages
	 */
	@Builder
	public record Response(
			UUID id,

			@Schema(description = "Card brand for UI icon", example = "Visa")
			String cardType,

			@Schema(description = "Last 4 digits for masking", example = "4242")
			String last4,

			@Schema(description = "Expiry month", example = "12")
			String expiryMonth,

			@Schema(description = "Expiry year", example = "2028")
			String expiryYear,

			@Schema(description = "Is this the primary card?")
			boolean isDefault,

			@Schema(description = "When the card was linked")
			OffsetDateTime linkedAt
	) {
	}

	/**
	 * Internal object used during Webhook processing
	 */
	@Builder
	public record YooKassaPaymentMethod(
			String id,         // The "pm-xxxx" token from YooMoney
			String type,       // "bank_card"
			CardDetails card,
			boolean saved      // Confirmation that the token is reusable
	) {
	}

	public record CardDetails(
			String first6,
			String last4,
			String expiryMonth,
			String expiryYear,
			String cardType,   // "Visa", "MasterCard", "MIR"
			String issuerName
	) {
	}
}
