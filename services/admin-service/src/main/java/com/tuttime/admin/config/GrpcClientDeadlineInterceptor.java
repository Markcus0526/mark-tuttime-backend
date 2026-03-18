package com.tuttime.admin.config;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.TimeUnit;

/**
 * Global gRPC client interceptor that enforces deadlines on all outgoing gRPC calls.
 *
 * <p>Without explicit deadlines, gRPC calls can hang indefinitely when the remote service
 * is unresponsive. The deadline is configurable via {@code grpc.client-deadline-seconds} (default: 60s).
 */
@Slf4j
@GrpcGlobalClientInterceptor
public class GrpcClientDeadlineInterceptor implements ClientInterceptor {

    @Value("${grpc.client-deadline-seconds:60}")
    private long deadlineSeconds;

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {
        if (callOptions.getDeadline() == null) {
            callOptions = callOptions.withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS);
        }
        return next.newCall(method, callOptions);
    }
}
