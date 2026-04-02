package com.tuttime.pos.repository;

import com.tuttime.pos.entity.PosErrorDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PosErrorDetailRepository extends JpaRepository<PosErrorDetail, Integer> {

	// Fetch errors for a specific sync attempt
	List<PosErrorDetail> findAllByLogIdOrderByCreatedAtDesc(Integer logId);
}
