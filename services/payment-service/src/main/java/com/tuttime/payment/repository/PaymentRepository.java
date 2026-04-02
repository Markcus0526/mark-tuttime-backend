package com.tuttime.payment.repository;

import com.tuttime.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

	/**
	 * Used by the Webhook logic to find the specific payment record
	 * associated with a Booking ID.
	 */
	Optional<Payment> findByBookingId(UUID bookingId);

	/**
	 * Used to verify if a Stripe Session already exists to prevent
	 * creating multiple sessions for the same booking.
	 */
	Optional<Payment> findByStripeSessionId(String stripeSessionId);
}
