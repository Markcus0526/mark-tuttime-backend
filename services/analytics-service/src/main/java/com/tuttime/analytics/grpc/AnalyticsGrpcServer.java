package com.tuttime.analytics.grpc;

import com.tuttime.analytics.entity.UserReliabilityIndex;
import com.tuttime.analytics.service.AnalyticsService;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.common.proto.AnalyticsServiceGrpc;
import com.tuttime.common.proto.UserScoreRequest;
import com.tuttime.common.proto.UserScoreResponse;
import com.tuttime.common.proto.UserServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
public class AnalyticsGrpcServer extends AnalyticsServiceGrpc.AnalyticsServiceImplBase {

	private final AnalyticsService analyticsService;

	@Override
	public void getUserScore(UserScoreRequest req, StreamObserver<UserScoreResponse> responseObserver) {
		try {
			UserReliabilityIndex stats = analyticsService.getUserReliability(UUID.fromString(req.getUserId()));

			UserScoreResponse response = UserScoreResponse.newBuilder()
					.setScore(stats != null ? stats.getReliabilityScore() : 50)
					.setRiskLevel(stats != null ? stats.getRiskLevel() : "STANDARD")
					.setRequiresFullPrepay(stats != null && stats.getReliabilityScore() < 25)
					.build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();

		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("<<< gRPC>>> Invalid argument: " + e.getMessage()).asRuntimeException());
		} catch (InvalidRequestException e) {
			// Map your custom business exception to NOT_FOUND instead of INTERNAL
			responseObserver.onError(Status.NOT_FOUND.withDescription("<<< gRPC>>> Invalid request: " + e.getMessage()).asRuntimeException());
		} catch (Exception e) {
			// Keep this for actual system failures (DB down, etc.)
			responseObserver.onError(Status.INTERNAL.withDescription("<<< gRPC>>> Unexpected error: " + e.getMessage()).asRuntimeException());
		}
	}

//	@Override
//	public void getBranchYield(YieldRequest req, StreamObserver<YieldResponse> responseObserver) {
//		var yield = analyticsService.getBranchYield(UUID.fromString(req.getBranchId()));
//
//		var response = YieldResponse.newBuilder()
//				.setOccupancyRate(yield.getOccupancyRate().floatValue())
//				.setDemandLevel(yield.getDemandLevel())
//				.setRecommendedDiscount(yield.getRecommendedDiscountPct())
//				.build();
//
//		responseObserver.onNext(response);
//		responseObserver.onCompleted();
//	}
}
