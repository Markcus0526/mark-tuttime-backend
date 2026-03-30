package com.tuttime.restaurant.repository;

import com.tuttime.restaurant.entity.Media;
import com.tuttime.restaurant.enums.EntityType;
import com.tuttime.restaurant.enums.MediaType;
import jakarta.transaction.Transactional;
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
	List<Media> findByEntityTypeAndEntityId(EntityType mediaEntity, UUID entityId);

	List<Media> findByEntityTypeAndEntityIdAndIsPrimaryTrue(EntityType mediaEntity, UUID entityId);

	@Modifying
	@Transactional
	@Query("UPDATE Media m SET m.isPrimary = false WHERE m.entityType = :entityType AND m.entityId = :entityId AND m.isPrimary = true")
	void clearPrimaryForEntity(EntityType entityType, UUID entityId);

	Optional<Media> findFirstByEntityTypeAndEntityIdAndMediaTypeAndIsPrimaryAndIsDeletedFalse(
			EntityType entityType, UUID entityId, MediaType mediaType, Boolean isPrimary
	);

	@Query("SELECT m FROM Media m " +
			"WHERE m.entityType = :entityType " +
			"AND m.entityId = :entityId " +
			"AND m.isMap = true " +
			"AND m.isDeleted = false")
	Optional<Media> findPrimaryMapByEntity(@Param("entityType") EntityType entityType, @Param("entityId") UUID entityId);

	// Find all media for an entity, excluding soft-deleted
	List<Media> findByEntityTypeAndEntityIdAndIsDeletedFalse(EntityType entityType, UUID entityId);

	// Find primary SVG map for an entity, excluding soft-deleted
	Optional<Media> findFirstByEntityTypeAndEntityIdAndMediaTypeAndIsPrimaryAndIsDeletedFalse(
			EntityType entityType,
			UUID entityId,
			MediaType mediaType,
			boolean isPrimary);

	// Custom query to find primary map by entity (used in setOrUpdateSvgMap)
	@Query("SELECT m FROM Media m WHERE m.entityType = :entityType AND m.entityId = :entityId AND m.isPrimary = true AND m.isMap = true AND m.isDeleted = false")
	Optional<Media> findPrimaryMapByEntityAndIsDeletedFalse(
			@Param("entityType") EntityType entityType,
			@Param("entityId") UUID entityId);
}
