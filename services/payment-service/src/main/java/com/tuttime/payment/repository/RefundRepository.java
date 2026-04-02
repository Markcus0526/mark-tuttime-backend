package com.tuttime.payment.repository;

import com.tuttime.payment.entity.Refund;
import com.tuttime.payment.entity.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

	Optional<Refund> findByRefundId(String refundId);

	List<Refund> findByPaymentId(Long paymentId);

	List<Refund> findByStatus(RefundStatus status);

	boolean existsByRefundId(String refundId);
}
