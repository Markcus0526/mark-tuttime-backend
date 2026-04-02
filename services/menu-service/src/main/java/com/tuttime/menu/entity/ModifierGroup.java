package com.tuttime.menu.entity;

import com.tuttime.common.entity.BaseUuidEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "modifier_groups")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ModifierGroup extends BaseUuidEntity {

	@Column(nullable = false)
	private String name;

	private String description;

	@Column(name = "min_select", nullable = false)
	@Builder.Default
	private Integer minSelect = 0;

	@Column(name = "max_select", nullable = false)
	@Builder.Default
	private Integer maxSelect = 1;

	@OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<Modifier> modifiers = new ArrayList<>();
}
