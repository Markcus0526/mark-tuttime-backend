package com.tuttime.restaurant.service;

import com.tuttime.restaurant.dto.BranchDto;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface BranchService {

	Page<BranchDto.BranchResponse> findAllBranches(UUID restaurantId, int page, int size);

	BranchDto.BranchResponse createBranch(BranchDto.BranchRequest request, UUID currentUserId, String userRoles);

	BranchDto.BranchResponse getBranchById(UUID branchId);

	BranchDto.BranchResponse updateBranch(UUID branchId, BranchDto.BranchRequest request, UUID currentUserId, String userRoles);

	void deleteBranch(UUID branchId, UUID currentUserId, String userRoles);

	boolean deleteAllByRestaurantId(UUID restaurantId);
}