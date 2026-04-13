package com.tuttime.booking.grpc.client;

import com.tuttime.common.proto.*;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserGrpcClient {

	@GrpcClient("user-service")
	private UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub;

}