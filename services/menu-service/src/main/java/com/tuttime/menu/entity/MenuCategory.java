package com.tuttime.menu.entity;

import com.tuttime.common.entity.BaseUuidAnDeleteEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "menu_categories")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class MenuCategory extends BaseUuidAnDeleteEntity {

	@Column(name = "branch_id", nullable = false)
	private UUID branchId; // Connected to external Branch Service

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "description")
	private String description;

	@OneToMany(mappedBy = "category", fetch = FetchType.EAGER)
	@Fetch(FetchMode.SUBSELECT) // Optimizes N+1 for collections
	@Builder.Default
	private Set<MenuItem> menuItems = new LinkedHashSet<>();

	@Column(name = "priority")
	@Builder.Default
	private Integer priority = 0; // Default to 0, lower numbers show up first

	@ManyToMany
	@JoinTable(
			name = "menu_category_modifier_groups",
			joinColumns = @JoinColumn(name = "menu_category_id"),
			inverseJoinColumns = @JoinColumn(name = "modifier_group_id")
	)
	@Builder.Default
	private Set<ModifierGroup> modifierGroups = new HashSet<>();
}