package com.tuttime.pos.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IikoConnector {
//	private final WebClient webClient;
//	private final SyncLogRepository syncLogRepository;
//
//	public Mono<String> getMenu(Long restaurantId, String apiUrl, String token) {
//		// Stub iiko API /api/1/menu
//		HttpHeaders headers = new HttpHeaders();
//		headers.setBearerAuth(token);
//
//		return webClient.get()
//				.uri(apiUrl + "/api/1/menu")
//				.headers(h -> h.putAll(headers))
//				.retrieve()
//				.bodyToMono(String.class)
//				.doOnSuccess(response -> logSync(restaurantId, "MENU", response, "SUCCESS"))
//				.doOnError(err -> logSync(restaurantId, "MENU", null, "ERROR", err.getMessage()));
//	}
//
//	public Mono<String> sendOrder(Long orderId, String apiUrl, String token, Map<String, Object> order) {
//		// Stub /api/1/orders
//		return webClient.post()
//				.uri(apiUrl + "/api/1/orders")
//				.header("Authorization", "Bearer " + token)
//				.bodyValue(order)
//				.retrieve()
//				.bodyToMono(String.class)
//				.doOnSuccess(res -> logSync(orderId, "ORDER", res, "SUCCESS"))
//				.doOnError(err -> logSync(orderId, "ORDER", null, "ERROR", err.getMessage()));
//	}
//
//	// Stub get statuses, tables
//
//	private void logSync(UUID restaurantId, String type, String response, String status, String... error) {
//		SyncLog log = new SyncLog();
//		log.setRestaurantId(restaurantId);
//		log.setType(type);
//		log.setStatus(status);
//		if (response != null) log.setPosResponse(response);
//		if (error.length > 0) log.setErrorMessage(error[0]);
//		syncLogRepository.save(log);
//	}
}
