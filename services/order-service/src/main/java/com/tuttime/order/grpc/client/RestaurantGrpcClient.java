package com.tuttime.order.grpc.client;

import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.common.grpc.GrpcExecutor;
import com.tuttime.common.proto.*;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.tuttime.common.constants.ServiceNames.RESTAURANT_SERVICE;

@Service
@Slf4j
public class RestaurantGrpcClient {

	@GrpcClient("restaurant-service")
	private RestaurantServiceGrpc.RestaurantServiceBlockingStub restaurantServiceBlockingStub;

	public TableHierarchyResponse getTableHierarchyDetail(TableHierarchyRequest request) {
		return GrpcExecutor.execute(RESTAURANT_SERVICE, "getTableHierarchyDetail", () ->
				restaurantServiceBlockingStub.withDeadlineAfter(3, TimeUnit.SECONDS)
						.getTableHierarchyDetail(request));
	}
}