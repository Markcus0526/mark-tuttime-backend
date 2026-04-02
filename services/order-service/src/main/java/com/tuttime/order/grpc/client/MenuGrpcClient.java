package com.tuttime.order.grpc.client;

import com.tuttime.common.proto.MenuServiceGrpc;
import com.tuttime.common.proto.PriceRequest;
import com.tuttime.common.proto.PriceResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class MenuGrpcClient {

	@GrpcClient("menu-service") // Name matches config in application.yml
	private MenuServiceGrpc.MenuServiceBlockingStub menuStub;

	public BigDecimal fetchVerifiedPrice(UUID menuItemId, List<UUID> modifierIds) {
		PriceRequest request = PriceRequest.newBuilder()
				.setItemId(menuItemId.toString())
				.addAllModifierIds(modifierIds.stream().map(UUID::toString).toList())
				.build();

		// gRPC Call
		PriceResponse response = menuStub.getPrice(request);
		return BigDecimal.valueOf(response.getBasePrice() + response.getModifiersTotal());
	}
}
