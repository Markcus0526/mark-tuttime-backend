package com.tuttime.restaurant.repository;

import com.tuttime.restaurant.entity.Media;
import com.tuttime.restaurant.entity.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {
    List<Media> findByEntityTypeAndEntityId(EntityType mediaEntity, Long entityId);

    List<Media> findByEntityTypeAndEntityIdAndIsPrimaryTrue(EntityType mediaEntity, Long entityId);
}
