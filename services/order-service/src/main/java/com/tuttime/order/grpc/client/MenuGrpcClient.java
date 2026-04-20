package com.tuttime.order.grpc.client;

import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.common.proto.*;
import com.tuttime.common.util.NumberUtil;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MenuGrpcClient {

	@GrpcClient("menu-service") // Name matches config in application.yml
	private MenuServiceGrpc.MenuServiceBlockingStub menuServiceBlockingStub;

	public BigDecimal getPriceForOrder(UUID menuItemId, List<UUID> modifierIds) {
		PriceRequest request = PriceRequest.newBuilder()
				.setMenuItemId(menuItemId.toString())
				.addAllModifierIds(modifierIds.stream().map(UUID::toString).toList())
				.build();

		// gRPC Call
		PriceResponse response = menuServiceBlockingStub.getPriceForOrder(request);
		return NumberUtil.stringToBigDecimal(response.getVerifiedTotalPrice());
	}

	public List<MenuItemDetailResponse> getMenuDetails(List<String> menuItemIds) {
		// 1. Build the request with repeated IDs
		MenuDetailsRequest request = MenuDetailsRequest.newBuilder()
				.addAllMenuItemIds(menuItemIds)
				.build();

		try {
			// 2. Call the server and return the items list
			MenuDetailsResponse response = menuServiceBlockingStub.getMenuDetails(request);
			return response.getItemsList();
		} catch (StatusRuntimeException e) {
			log.error("Failed to fetch menu details: {}", e.getStatus());
			throw new InvalidRequestException("Menu service is currently unavailable.");
		}
	}
}
