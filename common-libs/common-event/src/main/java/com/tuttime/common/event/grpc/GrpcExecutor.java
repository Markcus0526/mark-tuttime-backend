package com.tuttime.common.event.grpc;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@Slf4j
public class GrpcExecutor {

	private final RetryRegistry retryRegistry;

	/**
	 * Executes a gRPC call with Resilience4j retry.
	 * Uses the "grpc" Retry instance configured in application.yml.
	 */
	public <T> T execute(String serviceName, String methodName, Supplier<T> grpcCall) {
		Retry retry = retryRegistry.retry("grpc");
		Supplier<T> decoratedSupplier = Retry.decorateSupplier(retry, () -> {
			log.debug("Initiating gRPC call: {}.{}", serviceName, methodName);
			return grpcCall.get();
		});
		try {
			return decoratedSupplier.get();
		} catch (StatusRuntimeException e) {
			// The retry already attempted several times; log and rethrow.
			log.error("<<< gRPC >>> Persistent failure in {}.{} | Code: {} | Description: {}",
					serviceName, methodName, e.getStatus().getCode(), e.getStatus().getDescription());
			throw e;
		} catch (Exception e) {
			log.error("<<< gRPC >>> Non-gRPC error in {}.{}: {}", serviceName, methodName, e.getMessage());
			throw e;
		}
	}
}