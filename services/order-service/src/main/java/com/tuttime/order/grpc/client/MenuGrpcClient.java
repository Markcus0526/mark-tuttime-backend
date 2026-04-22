package com.tuttime.order.grpc.client;

import com.tuttime.common.event.grpc.GrpcExecutor;
import com.tuttime.common.event.proto.MenuDetailsRequest;
import com.tuttime.common.event.proto.MenuItemDetailResponse;
import com.tuttime.common.event.proto.MenuServiceGrpc;
import com.tuttime.common.event.proto.PriceRequest;
import com.tuttime.common.util.NumberUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.tuttime.common.constants.ServiceNames.MENU_SERVICE;

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

		String priceString = GrpcExecutor.execute(MENU_SERVICE, "getPriceForOrder", () ->
				menuServiceBlockingStub.withDeadlineAfter(3, TimeUnit.SECONDS)
						.getPriceForOrder(request).getVerifiedTotalPrice());

		return NumberUtil.stringToBigDecimal(priceString);
	}

	public List<MenuItemDetailResponse> getMenuDetails(List<String> menuItemIds) {
		// 1. Build the request with repeated IDs
		MenuDetailsRequest request = MenuDetailsRequest.newBuilder()
				.addAllMenuItemIds(menuItemIds)
				.build();

		return GrpcExecutor.execute(MENU_SERVICE, "getMenuDetails", () ->
				menuServiceBlockingStub.withDeadlineAfter(3, TimeUnit.SECONDS)
						.getMenuDetails(request).getItemsList());
	}
}
