package com.tuttime.payment.repository;

import com.tuttime.payment.entity.PaymentAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentAuditLogRepository extends JpaRepository<PaymentAuditLog, Long> {

	/**
	 * Critical for Idempotency: Checks if this specific Stripe Event ID
	 * has already been processed and logged.
	 */
	boolean existsByEventId(String eventId);
}
