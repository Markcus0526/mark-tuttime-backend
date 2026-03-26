package com.tuttime.pos.repository;

import com.tuttime.pos.entity.PosErrorDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PosErrorDetailRepository extends JpaRepository<PosErrorDetail, Long> {
	// Custom queries if needed
}
