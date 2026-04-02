package com.tuttime.payment.service.impl;

import com.tuttime.payment.entity.Payment;
import com.tuttime.payment.entity.Refund;
import com.tuttime.payment.entity.enums.RefundStatus;
import com.tuttime.payment.repository.PaymentRepository;
import com.tuttime.payment.repository.RefundRepository;
import com.tuttime.payment.service.RefundService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

	private final RefundRepository refundRepository;
	private final PaymentRepository paymentRepository;

	@Override
	@Transactional
	public Refund initiateRefund(Long paymentId, BigDecimal amount, String reason) {
		Payment payment = paymentRepository.findById(paymentId)
				.orElseThrow(() -> new IllegalArgumentException("Payment not found with id: " + paymentId));

		// Add refund policy enforcement here if needed

		Refund refund = Refund.builder()
				.refundId(UUID.randomUUID().toString())  // Generate unique refundId
				.payment(payment)
				.amount(amount)
				.reason(reason)
				.status(RefundStatus.COMPLETED)  // Use enum or constants ideally
				.build();

		return refundRepository.save(refund);
	}

	@Override
	public Optional<Refund> getRefundById(Long id) {
		return refundRepository.findById(id);
	}

	@Override
	public List<Refund> getRefundsByPaymentId(Long paymentId) {
		return refundRepository.findByPaymentId(paymentId);
	}

	@Override
	public List<Refund> getRefundsByStatus(RefundStatus status) {
		return refundRepository.findByStatus(status);
	}

	@Override
	@Transactional
	public Refund updateRefundStatus(Long refundId, RefundStatus status) {
		Refund refund = refundRepository.findById(refundId)
				.orElseThrow(() -> new IllegalArgumentException("Refund not found with id: " + refundId));
		refund.setStatus(status);
		return refundRepository.save(refund);
	}
}