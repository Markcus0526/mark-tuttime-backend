package com.tuttime.restaurant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode // Essential for composite keys
public class BranchTagId implements Serializable {

	@Column(name = "branch_id")
	private UUID branchId;

	@Column(name = "tag_id")
	private UUID tagId;
}
