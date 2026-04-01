package com.tuttime.menu.repository;

import com.tuttime.menu.entity.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface DiscountRepository extends JpaRepository<Discount, UUID> {
	List<Discount> findByReferenceIdAndIsActiveTrue(UUID referenceId);
}
