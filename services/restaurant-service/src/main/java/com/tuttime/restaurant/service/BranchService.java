package com.tuttime.restaurant.service;

import com.tuttime.restaurant.dto.BranchDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface BranchService {

	Page<BranchDto.BranchResponse> findAllBranches(UUID restaurantId, String query, Double minRating, Double maxRating, Pageable pageable);

	BranchDto.BranchResponse createBranch(BranchDto.BranchRequest request, UUID currentUserId, String userRoles);

	BranchDto.BranchDetailResponse getBranchById(UUID branchId);

	BranchDto.BranchResponse updateBranch(UUID branchId, BranchDto.BranchRequest request, UUID currentUserId, String userRoles);

	void deleteBranch(UUID branchId, UUID currentUserId, String userRoles);

	boolean deleteAllByRestaurantId(UUID restaurantId);

	Page<BranchDto.BranchResponse> findBranchesNearLocation(double latitude, double longitude, double radiusMeters, int page, int size);
}