package com.tuttime.payment.service;

import com.tuttime.payment.entity.Refund;
import com.tuttime.payment.enums.RefundStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface RefundService {

	Refund initiateRefund(Long paymentId, BigDecimal amount, String reason);

	Optional<Refund> getRefundById(Long id);

	List<Refund> getRefundsByPaymentId(Long paymentId);

	List<Refund> getRefundsByStatus(RefundStatus status);

	Refund updateRefundStatus(Long refundId, RefundStatus status);
}