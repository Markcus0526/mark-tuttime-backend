package com.tuttime.restaurant.repository;

import java.util.UUID;

import com.tuttime.restaurant.entity.Zone;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ZoneRepository extends JpaRepository<Zone, UUID> {
}
