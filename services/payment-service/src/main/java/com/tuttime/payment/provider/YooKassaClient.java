package com.tuttime.payment.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.loolzaaa.youkassa.client.ApiClient;
import ru.loolzaaa.youkassa.pojo.Amount;
import ru.loolzaaa.youkassa.pojo.Confirmation;
import ru.loolzaaa.youkassa.model.Payment; // In 0.2.0, the request is often the model itself
import ru.loolzaaa.youkassa.processors.PaymentProcessor;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class YooKassaClient {

	private final ApiClient apiKassaClient;

	public String createBindingPayment(UUID userId) {
		PaymentProcessor processor = new PaymentProcessor(apiKassaClient);
		String idempotencyKey = "bind-" + userId + "-" + UUID.randomUUID();

		// Build Amount
		Amount amount = Amount.builder()
				.value("1.00")
				.currency("RUB")
				.build();

		// Build Confirmation (top-level class, not inner class)
		Confirmation confirmation = Confirmation.builder()
				.type(Confirmation.Type.REDIRECT)  // Use the enum constant
				.returnUrl("https://eatnow.app")
				.build();

		// Build Payment request
		Payment request = Payment.builder()
				.amount(amount)
				.capture(true)
				.savePaymentMethod(true)
				.description("Card verification for Eat Now Profile")
				.confirmation(confirmation)  // Pass the Confirmation object
				.metadata(Map.of(
						"user_id", userId.toString(),
						"type", "CARD_BINDING"
				))
				.build();

		try {
			Payment yooPayment = processor.create(request, idempotencyKey);

			if (yooPayment.getConfirmation() != null &&
					yooPayment.getConfirmation().getType() == Confirmation.Type.REDIRECT) {
				return yooPayment.getConfirmation().getConfirmationUrl();
			}

			throw new RuntimeException("No redirect confirmation received from YooKassa");

		} catch (Exception e) {
			log.error("YooKassa binding error for user: {}", userId, e);
			throw new RuntimeException("YooKassa Binding Error: " + e.getMessage(), e);
		}
	}
}
