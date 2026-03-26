package com.tuttime.restaurant.repository;

import java.util.List;
import java.util.UUID;

import com.tuttime.restaurant.entity.EntityType;
import com.tuttime.restaurant.entity.Media;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaRepository extends JpaRepository<Media, UUID> {
	List<Media> findByEntityTypeAndEntityId(EntityType mediaEntity, UUID entityId);

	List<Media> findByEntityTypeAndEntityIdAndIsPrimaryTrue(EntityType mediaEntity, UUID entityId);
}
