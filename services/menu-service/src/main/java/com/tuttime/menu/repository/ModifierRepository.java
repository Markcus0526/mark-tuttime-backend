package com.tuttime.menu.repository;

import com.tuttime.menu.entity.Modifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModifierRepository extends JpaRepository<Modifier, Long> {
}
