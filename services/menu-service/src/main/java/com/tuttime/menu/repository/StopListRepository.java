package com.tuttime.menu.repository;

import com.tuttime.menu.entity.StopList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StopListRepository extends JpaRepository<StopList, UUID> {

	List<StopList> findByBranchId(UUID branchId);

	Optional<StopList> findByMenuItem_MenuItemIdAndBranchId(UUID menuItemId, UUID branchId);

	// Used to check if an item is currently active on a stop list
	boolean existsByMenuItem_MenuItemIdAndBranchId(UUID menuItemId, UUID branchId);
}
