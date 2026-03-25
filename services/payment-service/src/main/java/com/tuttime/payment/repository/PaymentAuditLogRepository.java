package com.tuttime.payment.repository;

import com.tuttime.payment.entity.PaymentAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentAuditLogRepository extends JpaRepository<PaymentAuditLog, Long> {

	List<PaymentAuditLog> findByPaymentId(Long paymentId);

	List<PaymentAuditLog> findByEventType(String eventType);
}