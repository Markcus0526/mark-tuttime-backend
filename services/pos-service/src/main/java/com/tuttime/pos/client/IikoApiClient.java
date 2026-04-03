package com.tuttime.pos.client;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class IikoApiClient {

	private static final Logger logger = LoggerFactory.getLogger(IikoApiClient.class);

	private final WebClient webClient;

	private final String apiBaseUrl = "https://api.iiko.example.com";  // Replace with real endpoint
	private final String apiKey = "your-iiko-api-key"; // Inject from config/env

	// Example: Authenticate and get token (if applicable)
	public Mono<String> authenticate() {
		// iiko usually uses API key in headers; adjust if OAuth or token needed
		return Mono.just(apiKey);
	}

	// Sync booking example
	public Mono<Void> syncBooking(Map<String, Object> bookingPayload) {
		return webClient.post()
				.uri(apiBaseUrl + "/bookings")
				.header("Authorization", "Bearer " + apiKey)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(bookingPayload)
				.retrieve()
				.bodyToMono(Void.class)
				.doOnSuccess(v -> logger.info("iiko booking sync successful"))
				.doOnError(e -> logger.error("iiko booking sync failed", e));
	}

	// Sync order example
	public Mono<Void> syncOrder(Map<String, Object> orderPayload) {
		return webClient.post()
				.uri(apiBaseUrl + "/orders")
				.header("Authorization", "Bearer " + apiKey)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(orderPayload)
				.retrieve()
				.bodyToMono(Void.class)
				.doOnSuccess(v -> logger.info("iiko order sync successful"))
				.doOnError(e -> logger.error("iiko order sync failed", e));
	}

	// Sync menu example
	public Mono<Void> syncMenu(Map<String, Object> menuPayload) {
		return webClient.post()
				.uri(apiBaseUrl + "/menu")
				.header("Authorization", "Bearer " + apiKey)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(menuPayload)
				.retrieve()
				.bodyToMono(Void.class)
				.doOnSuccess(v -> logger.info("iiko menu sync successful"))
				.doOnError(e -> logger.error("iiko menu sync failed", e));
	}
}