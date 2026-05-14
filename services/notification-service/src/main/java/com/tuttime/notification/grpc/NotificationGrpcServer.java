package com.tuttime.notification.grpc;

import com.google.protobuf.Empty;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;
import com.tuttime.common.enums.NotificationTemplateKey;
import com.tuttime.common.event.proto.NotificationServiceGrpc;
import com.tuttime.common.event.proto.SendNotificationRequest;
import com.tuttime.notification.service.NotificationOrchestrator;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
public class NotificationGrpcServer extends NotificationServiceGrpc.NotificationServiceImplBase {

	private final NotificationOrchestrator orchestrator;

	@Override
	public void sendNotification(SendNotificationRequest request, StreamObserver<Empty> responseObserver) {
		try {
			// 1. Convert google.protobuf.Struct to Map<String, Object>
			Map<String, Object> params = convertStructToMap(request.getParams());

			// 2. Trigger your existing logic
			orchestrator.handleNotification(
					UUID.randomUUID(), // New unique ID for this notification instance
					UUID.fromString(request.getUserId()),
					NotificationTemplateKey.valueOf(request.getTemplateKey()),
					params
			);

			responseObserver.onNext(Empty.getDefaultInstance());
			responseObserver.onCompleted();
		} catch (Exception e) {
			responseObserver.onError(io.grpc.Status.INTERNAL
					.withDescription("Failed to process notification: " + e.getMessage())
					.asRuntimeException());
		}
	}

	private Map<String, Object> convertStructToMap(Struct struct) throws Exception {
		String json = JsonFormat.printer().print(struct);
		return new com.fasterxml.jackson.databind.ObjectMapper()
				.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>(){});
	}
}
