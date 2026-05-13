package com.tuttime.restaurant.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "branch_tags", schema = "restaurant_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchTag {

	@EmbeddedId
	private BranchTagId id;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("branchId")
	@JoinColumn(name = "branch_id")
	private Branch branch;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("tagId")
	@JoinColumn(name = "tag_id")
	private Tag tag;
}
