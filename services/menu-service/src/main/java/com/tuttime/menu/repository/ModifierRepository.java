package com.tuttime.menu.repository;

import com.tuttime.menu.entity.Modifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModifierRepository extends JpaRepository<Modifier, UUID> {

	Optional<Modifier> findByIdAndDeletedFalse(UUID id);

	List<Modifier> findByModifierGroupId(UUID groupId);

	List<Modifier> findByModifierGroupIdAndDeletedFalse(UUID groupId);
}
