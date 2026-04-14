package com.tuttime.menu.entity;

import com.tuttime.common.entity.BaseDeleteEntity;
import com.tuttime.common.entity.BaseUuidAnDeleteEntity;
import com.tuttime.common.entity.BaseUuidEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.*;

@Entity
@Table(name = "modifier_groups")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ModifierGroup extends BaseUuidAnDeleteEntity {

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "description")
	private String description;

	@Column(name = "min_select", nullable = false)
	@Builder.Default
	private Integer minSelect = 0;

	@Column(name = "max_select", nullable = false)
	@Builder.Default
	private Integer maxSelect = 1;

	@OneToMany(mappedBy = "group", fetch = FetchType.EAGER)
	@Builder.Default
	private Set<Modifier> modifiers = new LinkedHashSet<>();
}
