package com.tuttime.menu.entity;

import com.tuttime.common.entity.BaseDeleteEntity;
import com.tuttime.common.entity.BaseUuidAnDeleteEntity;
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
public class MenuCategory extends BaseUuidAnDeleteEntity {

	@Column(name = "branch_id", nullable = false)
	private UUID branchId; // Connected to external Branch Service

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "description")
	private String description;

	@OneToMany(mappedBy = "category", fetch = FetchType.EAGER)
	@Builder.Default
	private Set<MenuItem> items = new LinkedHashSet<>();

	@Column(name = "priority")
	@Builder.Default
	private Integer priority = 0; // Default to 0, lower numbers show up first
}