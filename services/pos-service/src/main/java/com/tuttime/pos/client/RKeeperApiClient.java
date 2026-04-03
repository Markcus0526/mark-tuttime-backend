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
public class RKeeperApiClient {

	private static final Logger logger = LoggerFactory.getLogger(RKeeperApiClient.class);

	private final WebClient webClient;

	private final String apiBaseUrl = "https://api.rkeeper.example.com";  // Replace with actual URL
	private final String apiKey = "your-rkeeper-api-key"; // Inject from config/env

	// Example: Authenticate if required (token or API key)
	public Mono<String> authenticate() {
		// R-Keeper auth flow, if any, else just return API key
		return Mono.just(apiKey);
	}

	// Sync booking example
	public Mono<Void> syncBooking(Map<String, Object> bookingPayload) {
		return webClient.post()
				.uri(apiBaseUrl + "/booking/sync")
				.header("Authorization", "Bearer " + apiKey)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(bookingPayload)
				.retrieve()
				.bodyToMono(Void.class)
				.doOnSuccess(v -> logger.info("R-Keeper booking sync successful"))
				.doOnError(e -> logger.error("R-Keeper booking sync failed", e));
	}

	// Sync order example
	public Mono<Void> syncOrder(Map<String, Object> orderPayload) {
		return webClient.post()
				.uri(apiBaseUrl + "/order/sync")
				.header("Authorization", "Bearer " + apiKey)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(orderPayload)
				.retrieve()
				.bodyToMono(Void.class)
				.doOnSuccess(v -> logger.info("R-Keeper order sync successful"))
				.doOnError(e -> logger.error("R-Keeper order sync failed", e));
	}

	// Sync menu example
	public Mono<Void> syncMenu(Map<String, Object> menuPayload) {
		return webClient.post()
				.uri(apiBaseUrl + "/menu/sync")
				.header("Authorization", "Bearer " + apiKey)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(menuPayload)
				.retrieve()
				.bodyToMono(Void.class)
				.doOnSuccess(v -> logger.info("R-Keeper menu sync successful"))
				.doOnError(e -> logger.error("R-Keeper menu sync failed", e));
	}
}