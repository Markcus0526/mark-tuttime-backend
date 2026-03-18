package com.tuttime.admin.repository;

import com.tuttime.admin.entity.StatusLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Status Level Repository
 */
@Repository
public interface StatusLevelRepository extends JpaRepository<StatusLevel, Long> {

    Optional<StatusLevel> findByLevelName(String levelName);

    List<StatusLevel> findByIsActiveTrueOrderByDisplayOrderAsc();

    List<StatusLevel> findAllByOrderByDisplayOrderAsc();

    Optional<StatusLevel> findByMinApartBalance(BigDecimal minApartBalance);

    boolean existsByLevelName(String levelName);

    boolean existsByMinApartBalance(BigDecimal minApartBalance);
}

