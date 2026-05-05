package com.tuttime.restaurant.service;

import com.tuttime.restaurant.dto.BranchDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface BranchService {

	Page<BranchDto.BranchResponse> findAllBranches(UUID restaurantId, List<UUID> tagIds, String query,
	                                               BigDecimal minPrice, BigDecimal maxPrice,
	                                               Double minRating, Double maxRating,
	                                               Pageable pageable);

	BranchDto.BranchResponse createBranch(BranchDto.BranchRequest request, UUID currentUserId, String userRoles);

	BranchDto.BranchDetailResponse getBranchById(UUID branchId);

	BranchDto.BranchResponse updateBranch(UUID branchId, BranchDto.BranchRequest request, UUID currentUserId, String userRoles);

	void deleteBranch(UUID branchId, UUID currentUserId, String userRoles);

	void deleteAllByRestaurantId(UUID restaurantId);

	List<BranchDto.TagResponse> getAllTags();

	void syncBranchTags(UUID branchId, List<BranchDto.TagRequest> requests, UUID userId, String roles);

	boolean toggleBookmark(UUID branchId, UUID userId);

	Page<BranchDto.BranchResponse> searchMap(double latitude, double longitude, double radiusMeters,
	                                         List<UUID> tagIds, String query,
	                                         Pageable pageable,
	                                         UUID currentUserId);
}