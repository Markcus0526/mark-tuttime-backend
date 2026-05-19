package com.tuttime.restaurant.repository;

import com.tuttime.restaurant.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {

    // Logic #1 & #3: Find globally by name (ignore case)
    // Ensures a single master tag exists for everyone
    Optional<Tag> findByNameIgnoreCase(String name);
}