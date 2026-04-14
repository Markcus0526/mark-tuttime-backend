package com.tuttime.booking.grpc.client;

import com.tuttime.common.proto.*;
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
}