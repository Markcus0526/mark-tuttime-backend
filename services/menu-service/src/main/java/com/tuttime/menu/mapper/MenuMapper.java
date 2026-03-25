package com.tuttime.menu.mapper;

import com.tuttime.menu.dto.*;
import com.tuttime.menu.entity.*;

public class MenuMapper {

	public static MenuCategoryDto toDto(MenuCategory entity) {
		if (entity == null) return null;
		return MenuCategoryDto.builder()
				.id(entity.getId())
				.branchId(entity.getBranchId())
				.name(entity.getName())
				.description(entity.getDescription())
				.build();
	}

	public static MenuCategory toEntity(MenuCategoryDto dto) {
		if (dto == null) return null;
		return MenuCategory.builder()
				.id(dto.getId())
				.branchId(dto.getBranchId())
				.name(dto.getName())
				.description(dto.getDescription())
				.build();
	}

	public static MenuItemDto toDto(MenuItem entity) {
		if (entity == null) return null;
		return MenuItemDto.builder()
				.id(entity.getId())
				.categoryId(entity.getCategory().getId())
				.name(entity.getName())
				.description(entity.getDescription())
				.price(entity.getPrice())
				.isAvailable(entity.getIsAvailable())
				.build();
	}

	public static MenuItem toEntity(MenuItemDto dto, MenuCategory category) {
		if (dto == null) return null;
		return MenuItem.builder()
				.id(dto.getId())
				.category(category)
				.name(dto.getName())
				.description(dto.getDescription())
				.price(dto.getPrice())
				.isAvailable(dto.getIsAvailable())
				.build();
	}

	public static ModifierDto toDto(Modifier entity) {
		if (entity == null) return null;
		return ModifierDto.builder()
				.id(entity.getId())
				.name(entity.getName())
				.description(entity.getDescription())
				.priceAdjustment(entity.getPriceAdjustment())
				.build();
	}

	public static Modifier toEntity(ModifierDto dto) {
		if (dto == null) return null;
		return Modifier.builder()
				.id(dto.getId())
				.name(dto.getName())
				.description(dto.getDescription())
				.priceAdjustment(dto.getPriceAdjustment())
				.build();
	}

	public static StopListDto toDto(StopList entity) {
		if (entity == null) return null;
		return StopListDto.builder()
				.id(entity.getId())
				.menuItemId(entity.getMenuItem().getId())
				.branchId(entity.getBranchId())
				.startDate(entity.getStartDate())
				.endDate(entity.getEndDate())
				.reason(entity.getReason())
				.build();
	}

	public static StopList toEntity(StopListDto dto) {
		if (dto == null) return null;

		// TODO:
		return null;
//        MenuItem menuItem = menuitemService.getItemById(dto.getMenuItemId()); // assuming you have itemService
//        return StopList.builder()
//                .menuItem(menuItem)
//                .branchId(dto.getBranchId())
//                .startDate(dto.getStartDate())
//                .endDate(dto.getEndDate())
//                .reason(dto.getReason())
//                .build();
	}

	public static DiscountDto toDto(Discount entity) {
		if (entity == null) return null;
		return DiscountDto.builder()
				.name(entity.getName())
				.discountType(entity.getDiscountType())
				.amount(entity.getAmount())
				.appliesTo(entity.getAppliesTo())
				.referenceId(entity.getReferenceId())
				.startDate(entity.getStartDate())
				.endDate(entity.getEndDate())
				.isActive(entity.getIsActive())
				.build();
	}

	public static Discount toEntity(DiscountDto dto) {
		if (dto == null) return null;
		return Discount.builder()
				.name(dto.getName())
				.discountType(dto.getDiscountType())
				.amount(dto.getAmount())
				.appliesTo(dto.getAppliesTo())
				.referenceId(dto.getReferenceId())
				.startDate(dto.getStartDate())
				.endDate(dto.getEndDate())
				.isActive(dto.getIsActive())
				.build();
	}
}

