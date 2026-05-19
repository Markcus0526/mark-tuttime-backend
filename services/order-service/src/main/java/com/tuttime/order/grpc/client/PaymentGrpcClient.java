package com.tuttime.order.grpc.client;

import com.tuttime.common.event.grpc.GrpcExecutor;
import com.tuttime.common.event.proto.PaymentServiceGrpc;
import com.tuttime.common.event.proto.ProcessRefundRequest;
import com.tuttime.common.event.proto.ProcessRefundResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.tuttime.common.constants.ServiceNames.PAYMENT_SERVICE;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentGrpcClient {

    @Value("${grpc.client-deadline-seconds}")
    private int grpcDeadline;

    // This name must match the name in application.yml
    @GrpcClient("payment-service")
    private PaymentServiceGrpc.PaymentServiceBlockingStub paymentServiceBlockingStub;

    private final GrpcExecutor grpcExecutor;

    public ProcessRefundResponse processRefund(ProcessRefundRequest request) {
        return grpcExecutor.execute(PAYMENT_SERVICE, "processRefund", () ->
                paymentServiceBlockingStub
                        .withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
                        .processRefund(request));
    }
}