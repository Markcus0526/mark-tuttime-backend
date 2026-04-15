package com.tuttime.menu.repository;

import com.tuttime.menu.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaRepository extends JpaRepository<Media, UUID> {

	/**
	 * Finds the primary image URL for a given entity (Item, Category, or Modifier)
	 */
	@Query("SELECT m.url FROM Media m WHERE m.entityId = :entityId AND m.primaryEntity = true AND m.deleted = false")
	Optional<String> findPrimaryImageUrlByEntityId(@Param("entityId") UUID entityId);
}

