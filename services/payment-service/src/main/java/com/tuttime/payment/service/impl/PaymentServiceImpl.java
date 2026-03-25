package com.tuttime.payment.service.impl;

import com.tuttime.payment.entity.Payment;
import com.tuttime.payment.entity.PaymentAuditLog;
import com.tuttime.payment.enums.PaymentStatus;
import com.tuttime.payment.repository.PaymentAuditLogRepository;
import com.tuttime.payment.repository.PaymentRepository;
import com.tuttime.payment.service.PaymentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

	private final PaymentRepository paymentRepository;
	private final PaymentAuditLogRepository auditLogRepository;

	@Override
	@Transactional
	public Payment initiatePayment(Payment payment) {
		Payment savedPayment = paymentRepository.save(payment);
		logPaymentEvent(savedPayment.getId(), "PAYMENT_INITIATED", "Payment initiated with amount: " + payment.getAmount());
		return savedPayment;
	}

	@Override
	public Optional<Payment> getPaymentById(Long id) {
		return paymentRepository.findById(id);
	}

	@Override
	public List<Payment> getPaymentsByBookingId(Long bookingId) {
		return paymentRepository.findByBookingId(bookingId);
	}

	@Override
	public List<Payment> getPaymentsByStatus(PaymentStatus status) {
		return paymentRepository.findByStatus(status);
	}

	@Override
	@Transactional
	public Payment updatePaymentStatus(Long paymentId, PaymentStatus status) {
		Payment payment = paymentRepository.findById(paymentId)
				.orElseThrow(() -> new IllegalArgumentException("Payment not found"));
		payment.setStatus(status);
		Payment updatedPayment = paymentRepository.save(payment);
		logPaymentEvent(paymentId, "PAYMENT_STATUS_UPDATED", "Status updated to: " + status);
		return updatedPayment;
	}

	@Override
	@Transactional
	public void logPaymentEvent(Long paymentId, String eventType, String eventData) {
		PaymentAuditLog log = PaymentAuditLog.builder()
				.payment(paymentRepository.getReferenceById(paymentId))
				.eventType(eventType)
				.eventData(eventData)
				.build();
		auditLogRepository.save(log);
	}

	@Override
	public List<PaymentAuditLog> getAuditLogsForPayment(Long paymentId) {
		return auditLogRepository.findByPaymentId(paymentId);
	}
}
