package com.tuttime.analytics.grpc.client;

import com.tuttime.common.event.grpc.GrpcExecutor;
import com.tuttime.common.event.proto.*;
import com.tuttime.common.util.NumberUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.tuttime.common.constants.ServiceNames.MENU_SERVICE;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuGrpcClient {

	@Value("${grpc.client-deadline-seconds}")
	private int grpcDeadline;

	@GrpcClient("menu-service") // Name matches config in application.yml
	private MenuServiceGrpc.MenuServiceBlockingStub menuServiceBlockingStub;

	private final GrpcExecutor grpcExecutor;

	public GetMenuDetailsResponse getMenuDetails(List<String> menuItemIds) {
		// 1. Build the request with repeated IDs
		GetMenuDetailsRequest request = GetMenuDetailsRequest.newBuilder()
				.addAllMenuItemIds(menuItemIds)
				.build();

		return grpcExecutor.execute(MENU_SERVICE, "getMenuDetails", () ->
				menuServiceBlockingStub
						.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.getMenuDetails(request));
	}
}
