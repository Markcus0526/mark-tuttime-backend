package com.tuttime.integration.repository;

import com.tuttime.integration.entity.IntegrationCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface IntegrationCredentialsRepository extends JpaRepository<IntegrationCredentials, Long> {
  Optional<IntegrationCredentials> findByRestaurantIdAndPosSystem(Long restaurantId, IntegrationCredentials.PosSystem posSystem);
}
