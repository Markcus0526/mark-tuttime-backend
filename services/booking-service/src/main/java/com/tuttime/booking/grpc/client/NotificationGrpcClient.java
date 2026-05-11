package com.tuttime.booking.grpc.client;

import com.tuttime.common.event.grpc.GrpcExecutor;
import com.tuttime.common.event.proto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.tuttime.common.constants.ServiceNames.NOTIFICATION_SERVICE;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationGrpcClient {

	@Value("${grpc.client-deadline-seconds}")
	private int grpcDeadline;

	@GrpcClient("notification-service")
	private NotificationServiceGrpc.NotificationServiceBlockingStub notificationServiceBlockingStub;

	private final GrpcExecutor grpcExecutor;

	public void sendNotification(NotificationRequest request) {
		grpcExecutor.execute(NOTIFICATION_SERVICE, "sendNotification", () ->
				notificationServiceBlockingStub
						.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.sendNotification(request));
	}
}
