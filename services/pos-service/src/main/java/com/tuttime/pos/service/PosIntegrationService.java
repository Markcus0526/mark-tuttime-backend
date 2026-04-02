package com.tuttime.pos.service;

import com.tuttime.common.mapper.PosProtoMapper;
import com.tuttime.common.proto.OrderDetailsResponse;
import com.tuttime.common.proto.OrderRequest;
import com.tuttime.pos.entity.PosCredentials;
import com.tuttime.pos.grpc.client.OrderGrpcClient;
import com.tuttime.pos.mapper.PosMapper;
import com.tuttime.pos.repository.PosCredentialsRepository;
import com.tuttime.pos.repository.SyncLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PosIntegrationService {
	OrderGrpcClient orderGrpcClient;
	/**
	 * Fetches full order details via gRPC, converts to POS-specific Map,
	 * and prepares for terminal push.
	 */
	public void pushOrderToRestaurant(UUID orderId) {
		try {
			// 1. Fetch deep order details from Order Service
			OrderDetailsResponse response = orderGrpcClient.fetchOrderDetails(orderId);

			// 2. Convert Protobuf to Map<String, Object> for POS Payload
			Map<String, Object> posPayload = PosProtoMapper.toPosMap(response);

			// 3. Logic to send to physical iiko/R-Keeper terminal
			log.info("Order {} payload generated for POS: {}", orderId, posPayload.get("id"));
			// RestTemplate/WebClient call to physical terminal goes here

		} catch (Exception e) {
			log.error("Failed to push order {} to POS: {}", orderId, e.getMessage());
			throw new RuntimeException("POS Synchronization failed", e);
		}
	}
}
