package com.tuttime.menu.service.impl;

import com.tuttime.common.exception.ConflictException;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.menu.dto.DiscountDto;
import com.tuttime.menu.entity.Discount;
import com.tuttime.menu.entity.MenuCategory;
import com.tuttime.menu.entity.MenuItem;
import com.tuttime.menu.entity.enums.AppliesTo;
import com.tuttime.menu.entity.enums.DiscountType;
import com.tuttime.menu.grpc.client.RestaurantGrpcClient;
import com.tuttime.menu.mapper.DiscountMapper;
import com.tuttime.menu.repository.DiscountRepository;
import com.tuttime.menu.repository.MenuCategoryRepository;
import com.tuttime.menu.repository.MenuItemRepository;
import com.tuttime.menu.service.DiscountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountServiceImpl implements DiscountService {

	private final DiscountRepository discountRepository;
	private final DiscountMapper discountMapper;
	private final RestaurantGrpcClient restaurantGrpcClient;
	private final MenuCategoryRepository menuCategoryRepository;
	private final MenuItemRepository menuItemRepository;

	@Override
	@Transactional
	@CacheEvict(value = "branch_menus", allEntries = true)
	public DiscountDto.DiscountResponse createDiscount(DiscountDto.DiscountRequest request, UUID currentUserId, String userRoles) {
		// 1. Permission check on the branch
		restaurantGrpcClient.validateBranchPermission(request.branchId(), currentUserId, userRoles);

		// 2. Unique name within branch (case‑insensitive, soft‑delete aware)
		if (discountRepository.existsByBranchIdAndNameIgnoreCaseAndDeletedFalse(request.branchId(), request.name())) {
			throw new ConflictException("Discount with name '" + request.name() + "' already exists in this branch.");
		}

		// 3. Amount validation (already done via DTO annotations, but double‑check)
		validateDiscountAmount(request.discountType(), request.amount());

		// 4. Date range validation
		validateDates(request.startDate(), request.endDate(), request.active());

		// 5. Reference validation
		validateReference(request.appliesTo(), request.referenceId(), request.branchId());

		// 6. Build and save
		Discount discount = discountMapper.toEntity(request);
		discount.setBranchId(request.branchId());
		discount.setDeleted(false);
		Discount saved = discountRepository.save(discount);
		return discountMapper.toResponse(saved);
	}

	@Override
	@Transactional
	@CacheEvict(value = "branch_menus", allEntries = true)
	public DiscountDto.DiscountResponse updateDiscount(UUID id, DiscountDto.DiscountRequest request, UUID currentUserId, String userRoles) {
		Discount existing = discountRepository.findByIdAndDeletedFalse(id)
				.orElseThrow(() -> new InvalidRequestException("Discount not found: " + id));

		// Permission check using the existing branch (cannot change branch)
		restaurantGrpcClient.validateBranchPermission(existing.getBranchId(), currentUserId, userRoles);

		// If name changed, check uniqueness (excluding this discount)
		if (!request.name().equals(existing.getName())) {
			if (discountRepository.existsByBranchIdAndNameIgnoreCaseAndDeletedFalseAndIdNot(
					existing.getBranchId(), request.name(), id)) {
				throw new ConflictException("Discount with name '" + request.name() + "' already exists in this branch.");
			}
			existing.setName(request.name());
		}

		// Update other fields with validation
		existing.setDiscountType(request.discountType());
		existing.setAmount(request.amount());
		existing.setAppliesTo(request.appliesTo());
		existing.setReferenceId(request.referenceId());
		existing.setStartDate(request.startDate());
		existing.setEndDate(request.endDate());
		existing.setActive(request.active() != null ? request.active() : existing.getActive());

		// Validate amount again (if type changed)
		validateDiscountAmount(existing.getDiscountType(), existing.getAmount());

		// Validate dates again (if changed)
		validateDates(existing.getStartDate(), existing.getEndDate(), existing.getActive());

		// Validate reference after possible changes
		validateReference(existing.getAppliesTo(), existing.getReferenceId(), existing.getBranchId());

		return discountMapper.toResponse(discountRepository.save(existing));
	}

	@Override
	@Transactional(readOnly = true)
	public DiscountDto.DiscountResponse getDiscountById(UUID id) {
		Discount discount = discountRepository.findByIdAndDeletedFalse(id)
				.orElseThrow(() -> new InvalidRequestException("Discount not found: " + id));
		return discountMapper.toResponse(discount);
	}

	@Override
	@Transactional(readOnly = true)
	public List<DiscountDto.DiscountResponse> getActiveDiscountsForReference(UUID branchId, AppliesTo appliesTo, UUID referenceId) {
		OffsetDateTime now = OffsetDateTime.now();
		List<Discount> discounts;

		if (appliesTo == AppliesTo.ORDER) {
			// ORDER discounts apply to the whole branch (no referenceId)
			discounts = discountRepository.findByBranchIdAndAppliesToAndActiveTrueAndDeletedFalse(branchId, appliesTo);
		} else {
			// CATEGORY or ITEM – discounts tied to a specific referenceId
			if (referenceId == null) {
				throw new InvalidRequestException("referenceId is required for appliesTo = " + appliesTo);
			}
			discounts = discountRepository.findByBranchIdAndAppliesToAndReferenceIdAndActiveTrueAndDeletedFalse(branchId, appliesTo, referenceId);
		}

		// Additional runtime filtering by start/end dates (because dates are stored as OffsetDateTime)
		return discounts.stream()
				.filter(d -> (d.getStartDate() == null || d.getStartDate().isBefore(now)))
				.filter(d -> (d.getEndDate() == null || d.getEndDate().isAfter(now)))
				.map(discountMapper::toResponse)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	@CacheEvict(value = "branch_menus", allEntries = true)
	public void deleteDiscount(UUID id, UUID currentUserId, String userRoles) {
		Discount discount = discountRepository.findByIdAndDeletedFalse(id)
				.orElseThrow(() -> new InvalidRequestException("Discount not found: " + id));
		restaurantGrpcClient.validateBranchPermission(discount.getBranchId(), currentUserId, userRoles);
		discount.setDeleted(true);
		discount.setDeletedAt(OffsetDateTime.now());
		discountRepository.save(discount);
	}

	// ----- Private helpers -----
	private void validateDiscountAmount(DiscountType type, BigDecimal amount) {
		if (type == DiscountType.PERCENTAGE) {
			if (amount.compareTo(BigDecimal.ZERO) < 0 || amount.compareTo(new BigDecimal("100")) > 0) {
				throw new InvalidRequestException("Percentage discount must be between 0 and 100.");
			}
		} else if (type == DiscountType.FIXED_AMOUNT && amount.compareTo(BigDecimal.ZERO) < 0) {
			throw new InvalidRequestException("Fixed amount discount cannot be negative.");
		}
	}

	private void validateDates(OffsetDateTime startDate, OffsetDateTime endDate, Boolean active) {
		if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
			throw new InvalidRequestException("Start date must be before or equal to end date.");
		}
		if (Boolean.TRUE.equals(active) && endDate != null && endDate.isBefore(OffsetDateTime.now())) {
			throw new InvalidRequestException("Active discount cannot have an end date in the past.");
		}
	}

	private void validateReference(AppliesTo appliesTo, UUID referenceId, UUID branchId) {
		if (appliesTo == null) return;
		if (appliesTo == AppliesTo.CATEGORY) {
			MenuCategory category = menuCategoryRepository.findById(referenceId)
					.orElseThrow(() -> new InvalidRequestException("Referenced category does not exist."));
			if (!category.getBranchId().equals(branchId)) {
				throw new InvalidRequestException("Category does not belong to the specified branch.");
			}
		} else if (appliesTo == AppliesTo.ITEM) {
			MenuItem item = menuItemRepository.findById(referenceId)
					.orElseThrow(() -> new InvalidRequestException("Referenced menu item does not exist."));
			MenuCategory category = menuCategoryRepository.findById(item.getCategory().getId())
					.orElseThrow(() -> new InvalidRequestException("Associated category not found."));
			if (!category.getBranchId().equals(branchId)) {
				throw new InvalidRequestException("Menu item does not belong to the specified branch.");
			}
		}
		// ORDER type: no reference to validate
	}
}