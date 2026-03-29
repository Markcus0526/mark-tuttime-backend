package com.tuttime.restaurant.service;

import com.tuttime.restaurant.dto.BranchRequest;
import com.tuttime.restaurant.dto.BranchResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface BranchService {

	Page<BranchResponse> findAllBranches(UUID restaurantId, int page, int size);

	BranchResponse createBranch(UUID restaurantId, BranchRequest request);

	BranchResponse getBranchById(UUID branchId);

	BranchResponse updateBranch(UUID branchId, BranchRequest request);

	void deleteBranch(UUID branchId);
}