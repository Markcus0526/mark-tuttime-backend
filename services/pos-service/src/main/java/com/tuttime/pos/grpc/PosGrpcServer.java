package com.tuttime.pos.grpc;

import com.tuttime.common.proto.PosOrderRequest;
import com.tuttime.common.proto.PosOrderResponse;
import com.tuttime.common.proto.PosServiceGrpc;
import com.tuttime.pos.adapter.PosAdapter;
import com.tuttime.pos.adapter.impl.IikoPosAdapter;
import com.tuttime.pos.adapter.impl.RKeeperPosAdapter;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class PosGrpcServer extends PosServiceGrpc.PosServiceImplBase {

	private final IikoPosAdapter iikoPosAdapter;
	private final RKeeperPosAdapter rKeeperPosAdapter;

	@Override
	public void pushToKitchen(PosOrderRequest request, StreamObserver<PosOrderResponse> responseObserver) {
		log.info("Received order {} for table {} in zone {}",
				request.getOrderId(), request.getTableNumber(), request.getZoneName());

		try {
			// 1. Forward the order to the restaurant's local physical system
			String externalId = "";//TODO iikoPosAdapter.pushOrderToTerminal(request);

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