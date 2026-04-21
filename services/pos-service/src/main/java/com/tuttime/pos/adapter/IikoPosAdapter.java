package com.tuttime.pos.adapter;

import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.exception.PosSyncException;
import com.tuttime.common.proto.OrderDetailResponse;
import com.tuttime.pos.dto.liko.IikoErrorResponse;
import com.tuttime.pos.dto.liko.IikoNomenclatureResponse;
import com.tuttime.pos.entity.PosConfig;
import com.tuttime.pos.entity.enums.PosProvider;
import com.tuttime.pos.repository.PosMenuMappingRepository;
import com.tuttime.pos.service.PosMappingService;
import com.tuttime.pos.service.PosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class IikoPosAdapter implements PosAdapter {

	private final WebClient webClient; // Configured for iiko-ru.services
	private final PosService posService;
	private final PosMenuMappingRepository posMenuMappingRepository;
	private PosMappingService posMappingService;

	@Override
	public void reserveTable(PosConfig config, UUID tableId, UUID bookingId) {
		String url = "/api/1/reserve/create";

		// Contract based on iikoCloud API v1
		Map<String, Object> body = Map.of(
				"organizationId", config.getOrganizationId(),
				"terminalGroupId", config.getTerminalGroupId(),
				"reserve", Map.of(
						"tableIds", List.of(tableId.toString()),
						"estimatedStartTime", "2024-06-20 19:00:00", // Dynamic from Booking
						"comment", "TutTime Booking: " + bookingId
				)
		);

		executeSync(config, url, body, "RESERVE_TABLE", bookingId, "BOOKING");
	}

	@Override
	public PosProvider getProvider() {
		return PosProvider.IIKO;
	}

	@Override
	public void occupyTable(PosConfig config, UUID tableId, UUID bookingId) {
		// In iiko, 'occupying' a table often means moving the reserve to 'In Progress'
		// or creating an actual Order attached to that table.
		String url = "/api/1/order/create";
		Map<String, Object> body = Map.of(
				"organizationId", config.getOrganizationId(),
				"terminalGroupId", config.getTerminalGroupId(),
				"order", Map.of(
						"tableIds", List.of(tableId.toString()),
						"items", List.of() // Initially empty if the user hasn't ordered yet
				)
		);
		executeSync(config, url, body, "OCCUPY_TABLE", bookingId, "BOOKING");
	}

	@Override
	public void releaseTable(PosConfig config, UUID tableId, UUID bookingId) {
		// Releasing usually means canceling the existing reserve or clearing the table status.
		String url = "/api/1/reserve/cancel";
		Map<String, Object> body = Map.of(
				"organizationId", config.getOrganizationId(),
				"terminalGroupId", config.getTerminalGroupId(),
				"tableIds", List.of(tableId.toString())
		);
		executeSync(
				config,
				url,
				body,
				"RELEASE_TABLE",
				bookingId,   // entityId
				"BOOKING"    // entityType
		);
	}

	@Override
	public void sendOrder(PosConfig config, OrderDetailResponse order) {
		String url = "/api/1/order/create";

		List<Map<String, Object>> items = order.getItemsList().stream()
				.map(item -> {
					// 1. VALIDATION: Check if POS ID exists
					String posId = item.getPosId();

					if (!StringUtils.hasText(posId)) {
						// If the Order Service didn't provide it, try one last lookup in our mapping table
						posId = posMappingService.getExternalId(
								UUID.fromString(order.getBranchId()),
								item.getMenuItemId()
						);
					}

					// 2. CRITICAL ERROR: If still no ID, we cannot send this order
					if (!StringUtils.hasText(posId)) {
						throw new PosSyncException("Missing POS ID for item: " + item.getMenuItemName());
					}

					Map<String, Object> map = new HashMap<>();
					map.put("productId", posId);
					map.put("amount", item.getQuantity());
					return map;
				}).toList();

		Map<String, Object> body = Map.of(
				"organizationId", config.getOrganizationId(),
				"terminalGroupId", config.getTerminalGroupId(),
				"order", Map.of(
						"id", order.getOrderId(),
						"items", items,
						"tableIds", List.of(order.getTableId())
				)
		);

		executeSync(config, url, body, "SEND_ORDER", UUID.fromString(order.getOrderId()), "ORDER");
	}

	@Override
	public void cancelOrder(PosConfig config, UUID orderId, String reason) {
		String url = "/api/1/order/cancel";

		Map<String, Object> body = new HashMap<>();
		body.put("organizationId", config.getOrganizationId());
		body.put("orderId", orderId.toString());
		body.put("comment", "TutTime Cancel: " + reason);

		executeSync(config, url, body, "CANCEL_ORDER", orderId, "ORDER");
	}

	@Override
	public void refundOrder(PosConfig config, UUID orderId, BigDecimal amount, String reason) {
		String url = "/api/1/deliveries/cancel"; // Common for cloud delivery refunds

		Map<String, Object> body = new HashMap<>();
		body.put("organizationId", config.getOrganizationId());
		body.put("orderId", orderId.toString());
//		body.put("removalTypeId", config.getMetadata().get("defaultRefundReasonId")); TODO
		body.put("comment", "TutTime Refund: " + reason);

		executeSync(config, url, body, "REFUND_ORDER", orderId, "ORDER");
	}

	@Override
	public void syncMenu(PosConfig config) {
		String url = config.getApiEndpoint() + "/api/1/nomenclature";

		// WebClient configured in previous steps
		IikoNomenclatureResponse response = webClient.get()
				.uri(url)
				.header("Authorization", "Bearer " + config.getApiKey())
				.retrieve()
				.bodyToMono(IikoNomenclatureResponse.class)
				.block();

		if (response != null && response.getProducts() != null) {
			// Logic to update your pos_menu_mappings table
			posMappingService.updateIikoMappings(config.getBranchId(), response.getProducts());
		}
	}

	private void executeSync(PosConfig config, String url, Object body, String action, UUID entityId, String entityType) {
		try {
			String response = webClient.post()
					.uri(url)
					.header("Authorization", "Bearer " + config.getApiKey())
					.bodyValue(body)
					.retrieve()
					.onStatus(org.springframework.http.HttpStatusCode::is4xxClientError, clientResponse ->
							clientResponse.bodyToMono(IikoErrorResponse.class).flatMap(error -> {
								if (error.getMessage() != null && error.getMessage().contains("already reserved")) {
									return reactor.core.publisher.Mono.empty();
								}
								return reactor.core.publisher.Mono.error(new BusinessException(error.getMessage()));
							})
					)
					.bodyToMono(String.class)
					.block();

			// If response is null (due to Mono.empty()), it was an idempotent success
			String status = (response == null) ? "SUCCESS_ALREADY_SET" : "SUCCESS";

			// 2. Pass the parameters to the service
			posService.saveLog(
					config.getBranchId(),
					entityId,
					entityType,
					action,
					body,
					response,
					status
			);

		} catch (Exception e) {
			// 3. FIX: Catch block must also use the new 7-argument signature
			posService.saveLog(
					config.getBranchId(),
					entityId,
					entityType,
					action,
					body,
					e.getMessage(),
					"FAILED"
			);
			throw new PosSyncException("iiko API Error: " + e.getMessage(), e);
		}
	}
}

