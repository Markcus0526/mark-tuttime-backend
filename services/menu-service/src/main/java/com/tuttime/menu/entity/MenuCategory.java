package com.tuttime.menu.entity;

import com.tuttime.common.entity.BaseUuidEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "menu_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuCategory extends BaseUuidEntity {

	@Column(name = "branch_id", nullable = false)
	private UUID branchId; // Connected to external Branch Service

	@Column(nullable = false)
	private String name;

	private String description;

	@OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<MenuItem> items = new ArrayList<>();

	@Column(name = "priority")
	@Builder.Default
	private Integer priority = 0; // Default to 0, lower numbers show up first
}