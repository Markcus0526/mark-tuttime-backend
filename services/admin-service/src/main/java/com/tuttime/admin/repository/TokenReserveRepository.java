package com.tuttime.admin.repository;

import com.tuttime.admin.entity.TokenReserve;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Token Reserve entities
 */
@Repository
public interface TokenReserveRepository extends JpaRepository<TokenReserve, Long> {
    
    /**
     * Find token reserve by network
     */
    Optional<TokenReserve> findByNetwork(String network);
    
    /**
     * Check if reserve exists for network
     */
    boolean existsByNetwork(String network);
}

