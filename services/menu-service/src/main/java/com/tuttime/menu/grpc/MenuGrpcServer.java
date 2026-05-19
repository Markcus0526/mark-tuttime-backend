package com.tuttime.menu.grpc;

import com.tuttime.common.event.proto.*;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.menu.service.MenuGrpcDelegate;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class MenuGrpcServer extends MenuServiceGrpc.MenuServiceImplBase {

    private final MenuGrpcDelegate delegate;

    @Override
    public void getPriceForOrder(GetPriceForOrderRequest request, StreamObserver<GetPriceForOrderResponse> responseObserver) {
        try {
            responseObserver.onNext(delegate.getPriceForOrder(request));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (InvalidRequestException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("Unexpected error in getPriceForOrder", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getPreOrderMenu(GetPreOrderMenuRequest request, StreamObserver<GetPreOrderMenuResponse> responseObserver) {
        try {
            responseObserver.onNext(delegate.getPreOrderMenu(request));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (InvalidRequestException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("Unexpected error in getPreOrderMenu", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getMenuDetails(GetMenuDetailsRequest request, StreamObserver<GetMenuDetailsResponse> responseObserver) {
        try {
            responseObserver.onNext(delegate.getMenuDetails(request));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (InvalidRequestException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("Unexpected error in getMenuDetails", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getActiveOrderDiscounts(GetActiveOrderDiscountsRequest request,
                                        StreamObserver<GetActiveOrderDiscountsResponse> responseObserver) {
        try {
            GetActiveOrderDiscountsResponse response = delegate.getActiveOrderDiscounts(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (InvalidRequestException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("Unexpected error in getActiveOrderDiscounts", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}