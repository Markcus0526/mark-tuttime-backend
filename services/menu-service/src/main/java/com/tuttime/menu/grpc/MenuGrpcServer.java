package com.tuttime.menu.grpc;

import com.tuttime.common.proto.MenuServiceGrpc;
import com.tuttime.common.proto.PriceRequest;
import com.tuttime.common.proto.PriceResponse;
import com.tuttime.menu.repository.MenuItemRepository;
import com.tuttime.menu.repository.ModifierRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.math.BigDecimal;
import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
public class MenuGrpcServer extends MenuServiceGrpc.MenuServiceImplBase {

	private final MenuItemRepository menuItemRepository;
	private final ModifierRepository modifierRepository;

	@Override
	public void getPrice(PriceRequest request, StreamObserver<PriceResponse> responseObserver) {
		try {
			// 1. Fetch Base Item Price
			UUID itemId = UUID.fromString(request.getItemId());
			BigDecimal basePrice = menuItemRepository.findById(itemId)
					.map(item -> item.getPrice())
					.orElse(BigDecimal.ZERO);

			// 2. Calculate Modifiers Total
			double modifiersTotal = request.getModifierIdsList().stream()
					.map(UUID::fromString)
					.map(modId -> modifierRepository.findById(modId)
							.map(m -> m.getPriceAdjustment())
							.orElse(BigDecimal.ZERO))
					.reduce(BigDecimal.ZERO, BigDecimal::add)
					.doubleValue();

			// 3. Build and Send Response
			PriceResponse response = PriceResponse.newBuilder()
					.setBasePrice(basePrice.doubleValue())
					.setModifiersTotal(modifiersTotal)
					.build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();

		} catch (Exception e) {
			// Handle parsing errors or missing data
			responseObserver.onError(io.grpc.Status.INTERNAL
					.withDescription("Error calculating price: " + e.getMessage())
					.asRuntimeException());
		}
	}
}
