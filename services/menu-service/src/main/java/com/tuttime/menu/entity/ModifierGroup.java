package com.tuttime.menu.entity;

import com.tuttime.common.entity.BaseUuidEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "modifier_groups")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
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
