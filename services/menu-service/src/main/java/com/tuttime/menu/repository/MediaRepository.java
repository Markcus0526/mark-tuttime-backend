package com.tuttime.menu.repository;

import com.tuttime.menu.entity.Media;
import com.tuttime.menu.entity.enums.MediaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaRepository extends JpaRepository<Media, UUID> {

	/**
	 * Finds the primary image URL for a given entity (Item, Category, or Modifier)
	 */
	@Query("SELECT m.url FROM Media m WHERE m.entityId = :entityId AND m.primaryEntity = true AND m.deleted = false")
	Optional<String> findPrimaryImageUrlByEntityId(@Param("entityId") UUID entityId);

	@Query("SELECT m FROM Media m WHERE m.entityId = :entityId " +
			"AND (cast(:mediaType as string) IS NULL OR m.mediaType = :mediaType) " +
			"AND m.deleted = false")
	List<Media> findAllByEntityIdAndOptionalMediaType(
			@Param("entityId") UUID entityId,
			@Param("mediaType") MediaType mediaType
	);

	Optional<Media> findFirstByEntityIdAndPrimaryEntityTrue(UUID entityId);

	// Fetches every image/video linked to this item
	List<Media> findAllByEntityIdAndDeletedFalse(UUID entityId);

	@Modifying
	@Transactional
	@Query("UPDATE Media m SET m.primaryEntity = false WHERE m.entityId = :entityId AND m.primaryEntity = true")
	void resetPrimaryStatus(@Param("entityId") UUID entityId);

	Optional<Media> findFirstByEntityIdOrderByCreatedAtAsc(UUID entityId);

	void deleteByEntityIdAndMediaType(UUID entityId, MediaType mediaType);

	List<Media> findByEntityIdAndMediaType(UUID entityId, MediaType mediaType);
}

