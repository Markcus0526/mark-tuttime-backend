package com.tuttime.booking.grpc.client;

import com.tuttime.common.event.grpc.GrpcExecutor;
import com.tuttime.common.event.proto.MenuDetailsRequest;
import com.tuttime.common.event.proto.MenuItemDetailResponse;
import com.tuttime.common.event.proto.MenuRequest;
import com.tuttime.common.event.proto.MenuServiceGrpc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.tuttime.common.constants.ServiceNames.MENU_SERVICE;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuGrpcClient {

	@GrpcClient("menu-service")
	private MenuServiceGrpc.MenuServiceBlockingStub menuServiceBlockingStub;


	public List<MenuItemDetailResponse> getPreOrderMenu(UUID branchId) {
		MenuRequest request = MenuRequest.newBuilder()
				.setBranchId(branchId.toString())
				.build();
		return GrpcExecutor.execute(MENU_SERVICE, "getPreOrderMenu", () ->
				menuServiceBlockingStub.withDeadlineAfter(3, TimeUnit.SECONDS)
						.getPreOrderMenu(request).getItemsList());
//		return menuServiceBlockingStub.getPreOrderMenu(req).getItemsList();
	}

	public List<MenuItemDetailResponse> getMenuDetails(List<String> menuItemIds) {
		MenuDetailsRequest request = MenuDetailsRequest.newBuilder()
				.addAllMenuItemIds(menuItemIds)
				.build();

		return GrpcExecutor.execute(MENU_SERVICE, "getMenuDetails", () ->
				menuServiceBlockingStub.withDeadlineAfter(3, TimeUnit.SECONDS)
						.getMenuDetails(request).getItemsList());
//		try {
//			// 2. Call the server and return the items list
//			MenuDetailsResponse response = menuServiceBlockingStub.getMenuDetails(request);
//			return response.getItemsList();
//		} catch (StatusRuntimeException e) {
//			log.error("Failed to fetch menu details: {}", e.getStatus());
//			throw new InvalidRequestException("Menu service is currently unavailable.");
//		}
	}
}