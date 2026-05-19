package com.tuttime.menu.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "menu_item_tags", schema = "menu_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItemTag {
    @EmbeddedId
    private MenuItemTagId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("menuItemId")
    @JoinColumn(name = "menu_item_id")
    private MenuItem menuItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tagId")
    @JoinColumn(name = "tag_id")
    private MenuTag menuTag;
}
