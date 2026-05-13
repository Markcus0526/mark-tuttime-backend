package com.tuttime.payment.repository;

import com.tuttime.payment.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RefundRepository extends JpaRepository<Refund, UUID> {

	/**
	 * Retrieves all refunds associated with a specific payment.
	 */
	List<Refund> findByPaymentId(UUID paymentId);
}
