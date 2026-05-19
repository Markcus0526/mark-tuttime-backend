package com.tuttime.menu.repository;

import com.tuttime.menu.entity.ModifierGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModifierGroupRepository extends JpaRepository<ModifierGroup, UUID>, JpaSpecificationExecutor<ModifierGroup> {

    Optional<ModifierGroup> findByIdAndDeletedFalse(UUID id);

    // For creation
    boolean existsByBranchIdAndNameIgnoreCaseAndDeletedFalse(UUID branchId, String name);

    // For update
    boolean existsByBranchIdAndNameIgnoreCaseAndDeletedFalseAndIdNot(UUID branchId, String name, UUID excludeId);

    // Optionally find by branch
    List<ModifierGroup> findByBranchIdAndDeletedFalse(UUID branchId);

    List<ModifierGroup> findByBranchIdAndDeletedFalseOrderByNameAsc(UUID branchId);

}
