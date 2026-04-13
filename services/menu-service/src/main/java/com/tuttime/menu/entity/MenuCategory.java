package com.tuttime.menu.entity;

import com.tuttime.common.entity.BaseDeleteEntity;
import com.tuttime.common.entity.BaseUuidEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.*;

@Entity
@Table(name = "menu_categories")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class MenuCategory extends BaseDeleteEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "menu_category_id", nullable = false, updatable = false)
	private UUID menuCategoryId;

	@Column(name = "branch_id", nullable = false)
	private UUID branchId; // Connected to external Branch Service

	@Column(nullable = false)
	private String name;

	private String description;

	@OneToMany(mappedBy = "category", fetch = FetchType.EAGER)
	@Builder.Default
	private Set<MenuItem> items = new LinkedHashSet<>();

	@Column(name = "priority")
	@Builder.Default
	private Integer priority = 0; // Default to 0, lower numbers show up first
}