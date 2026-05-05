package com.tuttime.restaurant.repository;

import com.tuttime.restaurant.entity.BranchTag;
import com.tuttime.restaurant.entity.BranchTagId;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface BranchTagRepository extends JpaRepository<BranchTag, BranchTagId> {

	@Query("SELECT bt FROM BranchTag bt JOIN FETCH bt.tag WHERE bt.id.branchId = :branchId")
	List<BranchTag> findAllByBranchId(@Param("branchId") UUID branchId);

	@Modifying
	@Query("DELETE FROM BranchTag bt WHERE bt.id.branchId = :branchId")
	void deleteByBranchId(UUID branchId);

	boolean existsByTagId(UUID tagId);
}
