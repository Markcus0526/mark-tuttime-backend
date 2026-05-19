package com.tuttime.analytics.grpc.client;

import com.tuttime.common.event.grpc.GrpcExecutor;
import com.tuttime.common.event.proto.GetPosHealthRequest;
import com.tuttime.common.event.proto.GetPosHealthResponse;
import com.tuttime.common.event.proto.PosServiceGrpc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.tuttime.common.constants.ServiceNames.POS_SERVICE;

@Component
@RequiredArgsConstructor
@Slf4j
public class PosGrpcClient {

    @Value("${grpc.client-deadline-seconds}")
    private int grpcDeadline;

    @GrpcClient("pos-service")
    private PosServiceGrpc.PosServiceBlockingStub posServiceBlockingStub;

    private final GrpcExecutor grpcExecutor;

    public GetPosHealthResponse getPosHealth(UUID branchId) {
        GetPosHealthRequest request = GetPosHealthRequest.newBuilder()
                .setBranchId(branchId.toString())
                .build();

        return grpcExecutor.execute(POS_SERVICE, "getPosHealth", () ->
                posServiceBlockingStub
                        .withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
                        .getPosHealth(request));
    }
}