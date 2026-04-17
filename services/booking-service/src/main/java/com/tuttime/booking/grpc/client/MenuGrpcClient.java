package com.tuttime.booking.grpc.client;

import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.common.proto.*;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MenuGrpcClient {

	@GrpcClient("menu-service")
	private MenuServiceGrpc.MenuServiceBlockingStub menuServiceBlockingStub;


	public List<MenuItemDetail> getPreOrderMenu(UUID branchId) {
		MenuRequest req = MenuRequest.newBuilder()
				.setBranchId(branchId.toString())
				.build();
		return menuServiceBlockingStub.getPreOrderMenu(req).getItemsList();
	}

	public List<MenuItemDetail> getMenuDetails(List<String> menuItemIds) {
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