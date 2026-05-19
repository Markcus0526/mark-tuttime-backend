package com.tuttime.restaurant.repository;

import com.tuttime.restaurant.entity.Media;
import com.tuttime.restaurant.entity.enums.MediaType;
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

    Optional<Media> findByIdAndDeletedFalse(UUID mediaId);

    List<Media> findByEntityIdAndMediaType(UUID entityId, MediaType mediaType);

    // Find primary SVG map for an entity, excluding soft-deleted
    Optional<Media> findFirstByEntityIdAndMediaTypeAndDeletedFalse(UUID entityId, MediaType mediaType);

    @Query("SELECT m.url FROM Media m WHERE m.entityId = :entityId AND m.primaryEntity = true AND m.deleted = false")
    Optional<String> findPrimaryImageUrlByEntityId(@Param("entityId") UUID entityId);

    @Modifying
    @Query("UPDATE Media m SET m.deleted = true, m.deletedAt = CURRENT_TIMESTAMP " +
            "WHERE m.id = :entityId AND m.deleted = false")
    void deleteAllByEntityId(@Param("entityId") UUID entityId);

    @Query("SELECT m FROM Media m WHERE m.entityId = :entityId " +
            "AND (cast(:mediaType as string) IS NULL OR m.mediaType = :mediaType) " +
            "AND m.deleted = false")
    List<Media> findAllByEntityIdAndOptionalMediaType(
            @Param("entityId") UUID entityId,
            @Param("mediaType") MediaType mediaType
    );

    @Modifying
    @Transactional
    @Query("UPDATE Media m SET m.primaryEntity = false WHERE m.entityId = :entityId AND m.primaryEntity = true")
    void resetPrimaryStatus(@Param("entityId") UUID entityId);

    Optional<Media> findFirstByEntityIdOrderByCreatedAtAsc(UUID entityId);

    // Fetches every image/video linked to this item
    @Query("SELECT m FROM Media m WHERE m.entityId = :entityId " +
            "AND m.mediaType IN (com.tuttime.restaurant.entity.enums.MediaType.IMAGE, com.tuttime.restaurant.entity.enums.MediaType.VIDEO) " +
            "AND m.deleted = false")
    List<Media> findAllImageAndVideoByEntityId(UUID entityId);

    void deleteByEntityIdAndMediaType(UUID entityId, MediaType mediaType);
}
