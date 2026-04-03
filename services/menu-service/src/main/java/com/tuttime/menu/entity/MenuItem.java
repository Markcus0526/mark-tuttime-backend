package com.tuttime.menu.entity;

import com.tuttime.common.entity.BaseUuidEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "menu_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItem extends BaseUuidEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id", nullable = false)
	private MenuCategory category;

	@Column(nullable = false)
	private String name;

	private String description;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	@Column(name = "is_available")
	@Builder.Default
	private Boolean isAvailable = true;

	@Column(name = "priority")
	@Builder.Default
	private Integer priority = 0;

	@ManyToMany
	@JoinTable(
			name = "menu_item_modifier_groups",
			joinColumns = @JoinColumn(name = "menu_item_id"),
			inverseJoinColumns = @JoinColumn(name = "modifier_group_id")
	)
	@Builder.Default
	private Set<ModifierGroup> modifierGroups = new HashSet<>();
}