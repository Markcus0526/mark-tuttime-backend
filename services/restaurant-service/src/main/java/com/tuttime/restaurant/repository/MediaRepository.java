package com.tuttime.restaurant.repository;

import com.tuttime.restaurant.entity.Media;
import com.tuttime.restaurant.entity.enums.EntityType;
import com.tuttime.restaurant.entity.enums.MediaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaRepository extends JpaRepository<Media, UUID> {

	Optional<Media> findByMediaIdAndDeletedFalse(UUID mediaId);

	// Find all media for an entity, excluding soft-deleted
	List<Media> findAllByEntityTypeAndEntityIdAndDeletedFalse(EntityType entityType, UUID entityId);

	// Find primary SVG map for an entity, excluding soft-deleted
	Optional<Media> findFirstByEntityTypeAndEntityIdAndMediaTypeAndPrimaryEntityAndDeletedFalse(
			EntityType entityType,
			UUID entityId,
			MediaType mediaType,
			boolean primaryEntity);

	// Custom query to find primary map by entity (used in setOrUpdateSvgMap)
	@Query("SELECT m FROM Media m " +
			"WHERE m.entityType = :entityType AND m.entityId = :entityId AND m.primaryEntity = true AND m.mapEntity = true AND m.deleted = false")
	Optional<Media> findPrimaryMapByEntityAndDeletedFalse(
			@Param("entityType") EntityType entityType,
			@Param("entityId") UUID entityId);

	@Modifying
	@Query("UPDATE Media m SET m.deleted = true, m.deletedAt = CURRENT_TIMESTAMP " +
			"WHERE m.entityType = :entityType AND m.entityId = :entityId AND m.deleted = false")
	void deleteAllByEntityTypeAndEntityId(@Param("entityType") EntityType entityType, @Param("entityId") UUID entityId);
}
