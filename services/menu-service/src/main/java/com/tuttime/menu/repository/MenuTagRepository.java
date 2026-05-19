package com.tuttime.menu.repository;

import com.tuttime.menu.entity.MenuTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MenuTagRepository extends JpaRepository<MenuTag, UUID> {

    // Logic #1 & #3: Find globally by name (ignore case)
    // Ensures a single master tag exists for everyone
    Optional<MenuTag> findByNameIgnoreCase(String name);
}