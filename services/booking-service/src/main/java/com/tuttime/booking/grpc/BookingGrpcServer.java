package com.tuttime.booking.grpc;

import com.tuttime.booking.service.BookingGrpcDelegate;
import com.tuttime.common.event.proto.*;
import com.tuttime.common.exception.InvalidRequestException;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class BookingGrpcServer extends BookingServiceGrpc.BookingServiceImplBase {

    private final BookingGrpcDelegate delegate;

    @Override
    public void getCustomerRetention(GetCustomerRetentionRequest request,
                                     StreamObserver<GetCustomerRetentionResponse> responseObserver) {
        try {
            responseObserver.onNext(delegate.getCustomerRetention(request));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (InvalidRequestException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("Unexpected error in getCustomerRetention", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getTopCustomers(GetTopCustomersRequest request,
                                StreamObserver<GetTopCustomersResponse> responseObserver) {
        try {
            responseObserver.onNext(delegate.getTopCustomers(request));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (InvalidRequestException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("Unexpected error in getTopCustomers", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getFunnelCounts(GetFunnelCountsRequest request,
                                StreamObserver<GetFunnelCountsResponse> responseObserver) {
        try {
            responseObserver.onNext(delegate.getFunnelCounts(request));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (InvalidRequestException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("Unexpected error in getFunnelCounts", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}
