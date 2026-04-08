package com.tuttime.order.grpc.client;

import com.tuttime.common.proto.MenuServiceGrpc;
import com.tuttime.common.proto.PriceRequest;
import com.tuttime.common.proto.PriceResponse;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class MenuGrpcClient {

	@GrpcClient("menu-service") // Name matches config in application.yml
	private MenuServiceGrpc.MenuServiceBlockingStub menuServiceBlockingStub;

	public BigDecimal getPriceForOrder(UUID menuItemId, List<UUID> modifierIds) {
		PriceRequest request = PriceRequest.newBuilder()
				.setItemId(menuItemId.toString())
				.addAllModifierIds(modifierIds.stream().map(UUID::toString).toList())
				.build();

		// gRPC Call
		PriceResponse response = menuServiceBlockingStub.getPriceForOrder(request);
		return BigDecimal.valueOf(response.getBasePrice() + response.getModifiersTotal());
	}
}
