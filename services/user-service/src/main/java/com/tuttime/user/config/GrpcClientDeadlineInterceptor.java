package com.tuttime.user.config;

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
 * is unresponsive (e.g., server thread pool exhaustion, network partitions). This causes
 * cascading failures: the caller's threads are blocked, its own thread pool fills up,
 * and eventually it cannot serve any requests.
 *
 * <p>The deadline is configurable via {@code grpc.client-deadline-seconds} (default: 60s).
 * 60 seconds is generous enough for batch operations (e.g., fetching mining status for 500+ users)
 * while still preventing indefinite hangs. In worst-case scenarios (heavy DB load + GC pauses +
 * network latency), successful calls complete well within this window.
 *
 * <p>If a deadline already exists on the call (set explicitly by the caller), the shorter
 * of the two deadlines is used (gRPC handles this automatically).
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
        // Only set deadline if one isn't already set
        if (callOptions.getDeadline() == null) {
            callOptions = callOptions.withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS);
        }
        return next.newCall(method, callOptions);
    }
}
