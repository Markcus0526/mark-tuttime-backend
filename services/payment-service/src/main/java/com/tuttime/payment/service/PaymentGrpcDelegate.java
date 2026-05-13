package com.tuttime.payment.service;

import com.tuttime.common.event.proto.GetPaymentCountsRequest;
import com.tuttime.common.event.proto.GetPaymentCountsResponse;
import com.tuttime.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentGrpcDelegate {

	private final PaymentRepository paymentRepository;

	@Transactional(readOnly = true)
	public GetPaymentCountsResponse getPaymentCounts(GetPaymentCountsRequest request) {
		UUID branchId = UUID.fromString(request.getBranchId());
		LocalDate start = LocalDate.parse(request.getStartDate());
		LocalDate end = LocalDate.parse(request.getEndDate());
		long payments = paymentRepository.countPayments(branchId, start, end);
		return GetPaymentCountsResponse.newBuilder().setPayments(payments).build();
	}
}