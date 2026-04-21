package com.tuttime.pos.grpc;

import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.common.proto.KitchenOrderRequest;
import com.tuttime.common.proto.KitchenOrderResponse;
import com.tuttime.common.proto.PosServiceGrpc;
import com.tuttime.common.util.NumberUtil;
import com.tuttime.common.util.UuidUtil;
import com.tuttime.pos.adapter.PosAdapter;
import com.tuttime.pos.adapter.impl.IikoPosAdapter;
import com.tuttime.pos.adapter.impl.RKeeperPosAdapter;
import com.tuttime.pos.dto.PosConfigDto;
import com.tuttime.pos.entity.PosConfig;
import com.tuttime.pos.entity.PosLog;
import com.tuttime.pos.entity.PosSyncQueue;
import com.tuttime.pos.repository.PosConfigRepository;
import com.tuttime.pos.repository.PosLogRepository;
import com.tuttime.pos.repository.PosSyncQueueRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class PosGrpcServer extends PosServiceGrpc.PosServiceImplBase {

	private final PosConfigRepository posConfigRepository;
	private final PosLogRepository posLogRepository;
	private final PosSyncQueueRepository posSyncQueueRepository;
	private final IikoPosAdapter iikoPosAdapter;
	private final RKeeperPosAdapter rKeeperPosAdapter;

	@Override
	@Transactional
	public void pushToKitchen(KitchenOrderRequest request, StreamObserver<KitchenOrderResponse> responseObserver) {
		log.info("Incoming gRPC push for Order: {} (Branch: {})", request.getOrderId(), request.getBranchId());

		try {
			// 1. Lookup the Branch Configuration
			PosConfig config = posConfigRepository.findByBranchIdAndEnabledTrue(UuidUtil.toUuid(request.getBranchId()))
					.orElseThrow(() -> new InvalidRequestException("Branch POS not configured or disabled."));

			// 2. Map Protobuf Items to a JSON-serializable format for the Outbox (pos_logs)
			List<Map<String, Object>> serializableItems = request.getItemsList().stream()
					.map(item -> {
						Map<String, Object> map = new HashMap<>();
						map.put("menu_item_id", item.getMenuItemId());
						map.put("name", item.getName());
						map.put("quantity", item.getQuantity());
						map.put("price", NumberUtil.stringToBigDecimal(item.getPrice())); // Convert double to BigDecimal
						map.put("modifiers", item.getModifiersList()); // Simple list of strings
						return map;
					}).toList();

			// 3. Construct the Payload (Everything the IIKO/RKEEPER Strategy needs)
			Map<String, Object> fullPayload = new HashMap<>();
			fullPayload.put("order_id", request.getOrderId());
			fullPayload.put("booking_id", request.getBookingId());
			fullPayload.put("branch_id", request.getBranchId());
			fullPayload.put("items", serializableItems);

			// 4. Save Audit Log (The "Outbox")
			PosLog logEntry = PosLog.builder()
					.config(config)
					.entityType("ORDER")
					.entityId(UuidUtil.toUuid(request.getOrderId()))
					.status("PENDING")
					.requestPayload(fullPayload)
					.attemptCount(0)
					.build();
			PosLog savedLog = posLogRepository.save(logEntry);

			// 5. Queue for Background Worker
			PosSyncQueue queueEntry = PosSyncQueue.builder()
					.posLog(savedLog)
					.status("PENDING")
					.nextRetryAt(OffsetDateTime.now())
					.retryCount(0)
					.build();
			PosSyncQueue savedQueue = posSyncQueueRepository.save(queueEntry);

			// 6. Return Task ID back to Order Service
			responseObserver.onNext(KitchenOrderResponse.newBuilder()
					.setSuccess(true)
					.setPosTransactionId(savedQueue.getId().toString())
					.build());

		} catch (Exception e) {
			log.error("Failed to stage order for POS: {}", e.getMessage());
			responseObserver.onNext(KitchenOrderResponse.newBuilder()
					.setSuccess(false)
					.setErrorMessage("POS Staging Failed: " + e.getMessage())
					.build());
		} finally {
			responseObserver.onCompleted();
		}
	}
}