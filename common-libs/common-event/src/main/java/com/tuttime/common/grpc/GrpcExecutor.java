package com.tuttime.common.grpc;

import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import java.util.function.Supplier;

@Slf4j
public class GrpcExecutor {

	/**
	 * Executes a gRPC call.
	 * We do NOT catch the exception here because the GlobalExceptionHandler
	 * already handles StatusRuntimeException perfectly.
	 */
	public static <T> T execute(String serviceName, String methodName, Supplier<T> grpcCall) {
		try {
			log.debug("Initiating gRPC call: {}.{}", serviceName, methodName);
			return grpcCall.get();
		} catch (StatusRuntimeException e) {
			// We only log here for server-side traceability
			log.error("<<< gRPC >>> External Error in {}.{} | Code: {} | Description: {}",
					serviceName, methodName, e.getStatus().getCode(), e.getStatus().getDescription());

			// RE-THROW: Let the GlobalExceptionHandler convert this to your ErrorResponse
			throw e;
		} catch (Exception e) {
			log.error("<<< gRPC >>> Non-gRPC error in {}.{}: {}", serviceName, methodName, e.getMessage());
			throw e;
		}
	}
}
