package com.tuttime.menu.entity;

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
public class MenuItemTagId implements Serializable {

	@Column(name = "menu_item_id")
	private UUID menuItemId;

	@Column(name = "tag_id")
	private UUID tagId;
}
