package com.tuttime.menu.repository;

import com.tuttime.menu.entity.Modifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ModifierRepository extends JpaRepository<Modifier, UUID> {
	List<Modifier> findByGroup_ModifierGroupId(UUID groupId);
}
