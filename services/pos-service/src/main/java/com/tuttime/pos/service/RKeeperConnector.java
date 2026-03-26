package com.tuttime.pos.service;

import com.tuttime.pos.entity.SyncLog;
import com.tuttime.pos.repository.SyncLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class RKeeperConnector {
	private final WebClient webClient;
	private final SyncLogRepository syncLogRepository;

	public Mono<String> getMenu(Long restaurantId, String apiUrl, String apiKey) {
		// Stub R-Keeper API /menu
		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(apiKey, "");

		return webClient.get()
				.uri(apiUrl + "/menu")
				.headers(h -> h.putAll(headers))
				.retrieve()
				.bodyToMono(String.class)
				.doOnSuccess(response -> logSync(restaurantId, "MENU", response, "SUCCESS"))
				.doOnError(err -> logSync(restaurantId, "MENU", null, "ERROR", err.getMessage()));
	}

	public Mono<String> sendOrder(Long restaurantId, String apiUrl, String apiKey, Object order) {
		return webClient.post()
				.uri(apiUrl + "/order")
				.header("Authorization", "Basic " + apiKey)
				.bodyValue(order)
				.retrieve()
				.bodyToMono(String.class)
				.doOnSuccess(res -> logSync(restaurantId, "ORDER", res, "SUCCESS"))
				.doOnError(err -> logSync(restaurantId, "ORDER", null, "ERROR", err.getMessage()));
	}

	private void logSync(Long restaurantId, String type, String response, String status, String... error) {
		SyncLog log = new SyncLog();
		log.setRestaurantId(restaurantId);
		log.setType(type);
		log.setStatus(status);
		if (response != null) log.setPosResponse(response);
		if (error.length > 0) log.setErrorMessage(error[0]);
		syncLogRepository.save(log);
	}
}
