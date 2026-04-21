package com.tuttime.pos.grpc;

import com.tuttime.common.proto.PosOrderRequest;
import com.tuttime.common.proto.PosOrderResponse;
import com.tuttime.common.proto.PosServiceGrpc;
import com.tuttime.pos.adapter.PosAdapter;
import com.tuttime.pos.adapter.impl.IikoPosAdapter;
import com.tuttime.pos.adapter.impl.RKeeperPosAdapter;
import com.tuttime.pos.dto.PosConfigDto;
import com.tuttime.pos.entity.PosConfig;
import com.tuttime.pos.entity.PosLog;
import com.tuttime.pos.entity.PosSyncQueue;
import com.tuttime.pos.repository.PosConfigRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class PosGrpcServer extends PosServiceGrpc.PosServiceImplBase {

	private PosConfigRepository posConfigRepository;
	private final IikoPosAdapter iikoPosAdapter;
	private final RKeeperPosAdapter rKeeperPosAdapter;

	@Override
	@Transactional
	public void pushToKitchen(PosOrderRequest request, StreamObserver<PosOrderResponse> responseObserver) {
		log.info("Received order {} for table {} in zone {}",
				request.getOrderId(), request.getTableLabel(), request.getZoneName());

		try {
			// 1. Get the branch's specific IIKO/RKEEPER config
			PosConfig config = posConfigRepository.findByBranchIdAndEnabledTrue(request.getBranchId())
					.orElseThrow(() -> new InvalidRequestException("POS not configured for this branch"));

			// 2. Create the Audit Log (The "Outbox" entry)
			PosLog logEntry = PosLog.builder()
					.posSystem(config)
					.entityType("ORDER")
					.entityId(order.getId())
					.status("PENDING")
					.requestPayload(Map.of(
							"branch_id", order.getBranchId(),
							"order_data", order // The full order details
					))
					.build();
			posLogRepository.save(logEntry);

			// 3. Queue it for the background worker
			PosSyncQueue queueEntry = PosSyncQueue.builder()
					.posLog(logEntry)
					.nextRetryAt(OffsetDateTime.now())
					.status("PENDING")
					.build();
			posSyncQueueRepository.save(queueEntry);

			log.info("Order {} queued for {} sync", order.getId(), config.getPosProvider());

			// 2. Build the successful response
			PosOrderResponse response = PosOrderResponse.newBuilder()
					.setSuccess(true)
					.setPosTransactionId(externalId)
					.build();

			responseObserver.onNext(response);
			log.info("Order {} successfully printed in kitchen.", request.getOrderId());

		} catch (Exception e) {
			log.error("Failed to push order to restaurant hardware: {}", e.getMessage());

			// 3. Notify Order Service of the failure
			PosOrderResponse errorResponse = PosOrderResponse.newBuilder()
					.setSuccess(false)
					.setErrorMessage("Kitchen printer unreachable: " + e.getMessage())
					.build();

			responseObserver.onNext(errorResponse);
		} finally {
			responseObserver.onCompleted();
		}
	}
}