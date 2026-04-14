package com.tuttime.gateway.grpc.client;

import com.tuttime.common.proto.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class UserGrpcClient {

	@GrpcClient("user-service")
	private UserServiceGrpc.UserServiceBlockingStub userBockingStub;

	@GrpcClient("user-service")
	private UserServiceGrpc.UserServiceStub userAsyncStub;

}
