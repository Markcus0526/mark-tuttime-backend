package com.tuttime.user.grpc.client;

import com.tuttime.common.exception.ConflictException;
import com.tuttime.common.proto.*;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static com.tuttime.common.util.DatetimeUtil.convertOffsetDatetimeToProtobuf;

@Service
@Slf4j
public class RestaurantGrpcClient {

	@GrpcClient("restaurant-service")
	private RestaurantServiceGrpc.RestaurantServiceBlockingStub restaurantServiceBlockingStub;

}
