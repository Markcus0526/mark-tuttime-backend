package com.tuttime.pos.grpc;

import com.tuttime.common.event.proto.*;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.pos.service.PosGrpcDelegate;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class PosGrpcServer extends PosServiceGrpc.PosServiceImplBase {

    private final PosGrpcDelegate delegate;

    @Override
    public void pushToKitchen(PushToKitchenRequest request, StreamObserver<PushToKitchenResponse> responseObserver) {
        try {
            PosGrpcDelegate.PosSyncResult result = delegate.pushToKitchen(request);
            responseObserver.onNext(PushToKitchenResponse.newBuilder()
                    .setSuccess(true)
                    .setPosTransactionId(result.posTransactionId())
                    .build());
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("<<< gRPC>>> Invalid argument: " + e.getMessage()).asRuntimeException());
        } catch (InvalidRequestException e) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("<<< gRPC>>> Invalid request: " + e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("<<< gRPC>>> Unexpected error: " + e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getPosHealth(GetPosHealthRequest request,
                             StreamObserver<GetPosHealthResponse> responseObserver) {
        try {
            responseObserver.onNext(delegate.getPosHealth(request));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (InvalidRequestException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("Unexpected error in getPosHealth", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}