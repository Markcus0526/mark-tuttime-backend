package com.tuttime.payment.service;

import com.tuttime.payment.entity.Payment;
import com.tuttime.payment.entity.PaymentAuditLog;
import com.tuttime.payment.enums.PaymentStatus;

import java.util.List;
import java.util.Optional;

public interface PaymentService {

	Payment initiatePayment(Payment payment);

	Optional<Payment> getPaymentById(Long id);

	List<Payment> getPaymentsByBookingId(Long bookingId);

	List<Payment> getPaymentsByStatus(PaymentStatus status);

	Payment updatePaymentStatus(Long paymentId, PaymentStatus status);

	void logPaymentEvent(Long paymentId, String eventType, String eventData);

	List<PaymentAuditLog> getAuditLogsForPayment(Long paymentId);
}
