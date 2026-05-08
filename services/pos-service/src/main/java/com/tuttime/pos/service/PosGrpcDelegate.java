package com.tuttime.pos.service;

import com.tuttime.common.event.proto.KitchenOrderRequest;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.common.util.NumberUtil;
import com.tuttime.common.util.UuidUtil;
import com.tuttime.pos.adapter.IikoPosAdapter;
import com.tuttime.pos.adapter.RKeeperPosAdapter;
import com.tuttime.pos.entity.PosConfig;
import com.tuttime.pos.entity.PosLog;
import com.tuttime.pos.entity.PosSyncQueue;
import com.tuttime.pos.repository.PosConfigRepository;
import com.tuttime.pos.repository.PosLogRepository;
import com.tuttime.pos.repository.PosSyncQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class PosGrpcDelegate {

	private final PosConfigRepository posConfigRepository;
	private final PosLogRepository posLogRepository;
	private final PosSyncQueueRepository posSyncQueueRepository;
	private final IikoPosAdapter iikoPosAdapter;
	private final RKeeperPosAdapter rKeeperPosAdapter;

	@Transactional
	public PosSyncResult pushToKitchen(KitchenOrderRequest request) {
		UUID branchId = UuidUtil.toUuid(request.getBranchId());

		// 1. Lookup the Branch Configuration
		PosConfig config = posConfigRepository.findByBranchIdAndEnabledTrue(UuidUtil.toUuid(request.getBranchId()))
				.orElseThrow(() -> new InvalidRequestException("Branch POS not configured or disabled."));

		// 2. Map Protobuf Items to a JSON-serializable format for the Outbox
		List<Map<String, Object>> serializableItems = request.getItemsList().stream()
				.map(item -> {
					Map<String, Object> map = new HashMap<>();
					map.put("menu_item_id", item.getMenuItemId());
					map.put("name", item.getName());
					map.put("quantity", item.getQuantity());
					map.put("price", NumberUtil.stringToBigDecimal(item.getPrice()));
					map.put("modifiers", item.getModifiersList());
					return map;
				}).toList();

		// 3. Construct the Payload
		Map<String, Object> fullPayload = new HashMap<>();
		fullPayload.put("order_id", request.getOrderId());
		fullPayload.put("booking_id", request.getBookingId());
		fullPayload.put("branch_id", request.getBranchId());
		fullPayload.put("items", serializableItems);

		UUID orderId = UuidUtil.toUuid(request.getOrderId());
		UUID bookingId = UuidUtil.toUuid(request.getBookingId());

		// 4. Save Audit Log (The "Outbox")
		PosLog logEntry = PosLog.builder()
				.branchId(config.getBranchId())
				.entityId(UuidUtil.toUuid(request.getOrderId()))
				.entityType("ORDER")
				.action("PUSH_TO_KITCHEN")
				.requestPayload(fullPayload)
				.status("PENDING")
				.attemptCount(0)
				.build();
		PosLog savedLog = posLogRepository.save(logEntry);

		// 5. Queue for Background Worker
		PosSyncQueue queueEntry = PosSyncQueue.builder()
				.branchId(branchId)
				.bookingId(bookingId)
				.orderId(orderId)
				.action("SEND_TO_KITCHEN")                   // action for the queue worker
				.payload(fullPayload)                        // same payload
				.status("PENDING")                           // queue status
				.retryCount(0)
				.nextRetryAt(OffsetDateTime.now())           // process immediately
				.build();
		PosSyncQueue savedQueue = posSyncQueueRepository.save(queueEntry);

		// 6. Return result (transaction commits after this method returns)
		return new PosSyncResult(savedQueue.getId().toString());
	}

	public record PosSyncResult(String posTransactionId) {}
}