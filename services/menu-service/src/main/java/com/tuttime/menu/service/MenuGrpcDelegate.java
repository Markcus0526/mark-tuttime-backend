package com.tuttime.menu.service;

import com.tuttime.common.event.proto.*;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.menu.dto.DiscountDto;
import com.tuttime.menu.entity.MenuCategory;
import com.tuttime.menu.entity.MenuItem;
import com.tuttime.menu.entity.Modifier;
import com.tuttime.menu.entity.enums.AppliesTo;
import com.tuttime.menu.repository.MenuCategoryRepository;
import com.tuttime.menu.repository.MenuItemRepository;
import com.tuttime.menu.repository.ModifierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuGrpcDelegate {

	private final MenuCategoryRepository menuCategoryRepository;
	private final MenuItemRepository menuItemRepository;
	private final ModifierRepository modifierRepository;
	private final DiscountService discountService;

	@Transactional(readOnly = true)
	public PriceResponse getPriceForOrder(PriceRequest request) {
		UUID itemId = UUID.fromString(request.getMenuItemId());
		BigDecimal totalPrice = menuItemRepository.findById(itemId)
				.map(MenuItem::getPrice)
				.orElseThrow(() -> new InvalidRequestException("Menu item not found"));

		if (!request.getModifierIdsList().isEmpty()) {
			BigDecimal modifiersTotal = request.getModifierIdsList().stream()
					.map(UUID::fromString)
					.map(id -> modifierRepository.findById(id)
							.map(Modifier::getPriceAdjustment)
							.orElse(BigDecimal.ZERO))
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			totalPrice = totalPrice.add(modifiersTotal);
		}

		return PriceResponse.newBuilder()
				.setVerifiedTotalPrice(totalPrice.toString())
				.build();
	}

	@Transactional(readOnly = true)
	public MenuResponse getPreOrderMenu(MenuRequest request) {
		UUID branchId = UUID.fromString(request.getBranchId());
		List<MenuCategory> categories = menuCategoryRepository.findAllByBranchIdOrderByPriorityAsc(branchId);
		MenuResponse.Builder responseBuilder = MenuResponse.newBuilder();

		for (MenuCategory category : categories) {
			for (MenuItem menuItem : category.getMenuItems()) {
				if (!menuItem.getAvailable()) continue;

				MenuItemDetailResponse.Builder itemBuilder = MenuItemDetailResponse.newBuilder()
						.setMenuItemId(menuItem.getId().toString())
						.setName(menuItem.getName())
						.setDescription(menuItem.getDescription() != null ? menuItem.getDescription() : "")
						.setBasePrice(menuItem.getPrice().toString())
						.setCategoryId(category.getId().toString())
						.setCategoryName(category.getName())
						.setAvailable(menuItem.getAvailable());

				if (menuItem.getModifierGroups() != null) {
					menuItem.getModifierGroups().forEach(group -> {
						ModifierGroupResponse.Builder groupBuilder = ModifierGroupResponse.newBuilder()
								.setGroupName(group.getName())
								.setMinSelect(group.getMinSelect())
								.setMaxSelect(group.getMaxSelect());

						group.getModifiers().forEach(opt -> groupBuilder.addOptions(
								ModifierOptionResponse.newBuilder()
										.setName(opt.getName())
										.setExtraPrice(opt.getPriceAdjustment().toString())
										.build()
						));
						itemBuilder.addModifierGroups(groupBuilder);
					});
				}
				responseBuilder.addItems(itemBuilder);
			}
		}

		return responseBuilder.build();
	}

	@Transactional(readOnly = true)
	public MenuDetailsResponse getMenuDetails(MenuDetailsRequest request) {
		List<UUID> uuids = request.getMenuItemIdsList().stream()
				.map(UUID::fromString)
				.toList();
		List<MenuItem> entities = menuItemRepository.findAllById(uuids);
		MenuDetailsResponse.Builder responseBuilder = MenuDetailsResponse.newBuilder();
		for (MenuItem entity : entities) {
			responseBuilder.addItems(mapToProto(entity));
		}
		return responseBuilder.build();
	}

	@Transactional(readOnly = true)
	public ActiveOrderDiscountsResponse getActiveOrderDiscounts(ActiveOrderDiscountsRequest request) {
		UUID branchId = UUID.fromString(request.getBranchId());
		// Fetch active order discounts (appliesTo = ORDER, referenceId = null)
		List<DiscountDto.DiscountResponse> discounts = discountService.getActiveDiscountsForReference(
				branchId, AppliesTo.ORDER, null);

		ActiveOrderDiscountsResponse.Builder responseBuilder = ActiveOrderDiscountsResponse.newBuilder();
		for (DiscountDto.DiscountResponse d : discounts) {
			responseBuilder.addDiscounts(ActiveDiscount.newBuilder()
					.setId(d.discountId().toString())
					.setName(d.name())
					.setDiscountType(d.discountType().name())
					.setAmount(d.amount().doubleValue())
					.setAppliesTo(d.appliesTo().name())
					.setStartDate(d.startDate() != null ? d.startDate().toString() : "")
					.setEndDate(d.endDate() != null ? d.endDate().toString() : "")
					.build());
		}
		return responseBuilder.build();
	}

	private MenuItemDetailResponse mapToProto(MenuItem entity) {
		MenuItemDetailResponse.Builder builder = MenuItemDetailResponse.newBuilder()
				.setMenuItemId(entity.getId().toString())
				.setName(entity.getName())
				.setDescription(entity.getDescription() != null ? entity.getDescription() : "")
				.setBasePrice(entity.getPrice().toString())
				.setCategoryId(entity.getCategory().getId().toString())
				.setCategoryName(entity.getCategory().getName())
				.setAvailable(entity.getAvailable());

		if (entity.getModifierGroups() != null) {
			entity.getModifierGroups().forEach(group -> {
				ModifierGroupResponse.Builder groupBuilder = ModifierGroupResponse.newBuilder()
						.setGroupId(group.getId().toString())
						.setGroupName(group.getName())
						.setMinSelect(group.getMinSelect())
						.setMaxSelect(group.getMaxSelect());

				if (group.getModifiers() != null) {
					group.getModifiers().forEach(modifier -> {
						groupBuilder.addOptions(ModifierOptionResponse.newBuilder()
								.setOptionId(modifier.getId().toString())
								.setName(modifier.getName())
								.setExtraPrice(modifier.getPriceAdjustment().toString())
								.build());
					});
				}
				builder.addModifierGroups(groupBuilder.build());
			});
		}
		return builder.build();
	}
}