package com.tuttime.restaurant.repository;

import java.util.List;
import java.util.UUID;

import com.tuttime.restaurant.enums.EntityType;
import com.tuttime.restaurant.entity.Media;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaRepository extends JpaRepository<Media, UUID> {
	List<Media> findByEntityTypeAndEntityId(EntityType mediaEntity, UUID entityId);

	List<Media> findByEntityTypeAndEntityIdAndIsPrimaryTrue(EntityType mediaEntity, UUID entityId);

	@Modifying
	@Transactional
	@Query("UPDATE Media m SET m.isPrimary = false WHERE m.entityType = :entityType AND m.entityId = :entityId AND m.isPrimary = true")
	void clearPrimaryForEntity(EntityType entityType, UUID entityId);
}
