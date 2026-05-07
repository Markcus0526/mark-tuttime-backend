package com.tuttime.payment.repository;

import com.tuttime.payment.entity.UserPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PaymentMethodRepository extends JpaRepository<UserPaymentMethod, UUID> {

	List<UserPaymentMethod> findAllByUserIdAndActiveTrue(UUID userId);

	@Modifying
	@Query("UPDATE UserPaymentMethod p SET p.defaultMethod = false WHERE p.userId = :userId")
	void resetDefaultFlags(UUID userId);

	// Checks if the user has any existing saved cards
	boolean existsByUserId(UUID userId);
}
