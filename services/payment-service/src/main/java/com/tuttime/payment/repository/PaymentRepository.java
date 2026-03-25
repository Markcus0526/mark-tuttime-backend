package com.tuttime.payment.repository;

import com.tuttime.payment.entity.Payment;
import com.tuttime.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Optional<Payment> findByPaymentIntentId(String paymentIntentId);

	List<Payment> findByBookingId(Long bookingId);

	List<Payment> findByStatus(PaymentStatus status);

	boolean existsByPaymentIntentId(String paymentIntentId);
}
