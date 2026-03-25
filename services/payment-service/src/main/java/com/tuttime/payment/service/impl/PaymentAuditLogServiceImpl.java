package com.tuttime.payment.service.impl;

import com.tuttime.payment.entity.PaymentAuditLog;
import com.tuttime.payment.repository.PaymentAuditLogRepository;
import com.tuttime.payment.repository.PaymentRepository;
import com.tuttime.payment.service.PaymentAuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentAuditLogServiceImpl implements PaymentAuditLogService {

	private final PaymentAuditLogRepository auditLogRepository;
	private final PaymentRepository paymentRepository;

	@Override
	public PaymentAuditLog addAuditLog(Long paymentId, String eventType, String eventData) {
		var paymentRef = paymentRepository.getReferenceById(paymentId);

		PaymentAuditLog log = PaymentAuditLog.builder()
				.payment(paymentRef)
				.eventType(eventType)
				.eventData(eventData)
				.build();

		return auditLogRepository.save(log);
	}

	@Override
	public List<PaymentAuditLog> getLogsByPaymentId(Long paymentId) {
		return auditLogRepository.findByPaymentId(paymentId);
	}

	@Override
	public List<PaymentAuditLog> getLogsByEventType(String eventType) {
		return auditLogRepository.findByEventType(eventType);
	}
}
