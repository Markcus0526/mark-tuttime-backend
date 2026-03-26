package com.tuttime.pos.repository;

import com.tuttime.pos.entity.PosSystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PosSystemConfigRepository extends JpaRepository<PosSystemConfig, Long> {
	PosSystemConfig findByPosSystemAndEnabledTrue(String posSystem);
	Optional<PosSystemConfig> findByPosSystem(String posSystem);
}
