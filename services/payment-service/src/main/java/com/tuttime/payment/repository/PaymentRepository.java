package com.tuttime.payment.repository;

import com.tuttime.payment.entity.Payment;
import com.tuttime.payment.repository.projection.BranchPaymentSummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID>, JpaSpecificationExecutor<Payment> {

	/**
	 * Used by the Webhook logic to find the specific payment record
	 * associated with a Booking ID.
	 */
	Optional<Payment> findByBookingId(UUID bookingId);

	/**
	 * Used to verify if a Stripe Session already exists to prevent
	 * creating multiple sessions for the same booking.
	 */
	Optional<Payment> findByYookassaPaymentId(String yookassaPaymentId);

	@Query("SELECT COUNT(p.id) FROM Payment p WHERE p.branchId = :branchId AND DATE(p.createdAt) BETWEEN :start AND :end")
	long countPayments(UUID branchId, LocalDate start, LocalDate end);

	@Query(value = """
    SELECT
        COALESCE(SUM(CASE WHEN DATE(p.created_at) = CURRENT_DATE THEN p.amount ELSE 0 END), 0) AS todayTotal,
        COALESCE(COUNT(CASE WHEN DATE(p.created_at) = CURRENT_DATE THEN 1 END), 0) AS todayCount,
        COALESCE(SUM(CASE WHEN p.created_at >= CURRENT_DATE - INTERVAL '7 days' THEN p.amount ELSE 0 END), 0) AS weekTotal,
        COALESCE(COUNT(CASE WHEN p.created_at >= CURRENT_DATE - INTERVAL '7 days' THEN 1 END), 0) AS weekCount,
        COALESCE(SUM(CASE WHEN DATE_TRUNC('month', p.created_at) = DATE_TRUNC('month', CURRENT_DATE) THEN p.amount ELSE 0 END), 0) AS monthTotal,
        COALESCE(COUNT(CASE WHEN DATE_TRUNC('month', p.created_at) = DATE_TRUNC('month', CURRENT_DATE) THEN 1 END), 0) AS monthCount,
        COALESCE(SUM(CASE WHEN p.status = 'PENDING' THEN p.amount ELSE 0 END), 0) AS pendingTotal,
        COALESCE(COUNT(CASE WHEN p.status = 'PENDING' THEN 1 END), 0) AS pendingCount,
        COALESCE(SUM(CASE WHEN p.status = 'FAILED' THEN p.amount ELSE 0 END), 0) AS refundedTotal,
        COALESCE(COUNT(CASE WHEN p.status = 'FAILED' THEN 1 END), 0) AS refundedCount
    FROM payments p
    WHERE p.branch_id = :branchId
    """, nativeQuery = true)
	BranchPaymentSummaryProjection getBranchPaymentSummary(@Param("branchId") UUID branchId);

}
