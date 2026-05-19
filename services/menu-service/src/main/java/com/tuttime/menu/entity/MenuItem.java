package com.tuttime.menu.entity;

import com.tuttime.common.entity.BaseUuidAnDeleteEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "menu_items")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class MenuItem extends BaseUuidAnDeleteEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_category_id", nullable = false)
    private MenuCategory category;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "available")
    @Builder.Default
    private Boolean available = true;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0;

    /**
     * Many-to-Many relationship with Modifier Groups.
     * This links specific customization rules directly to this item.
     */
    @ManyToMany
    @JoinTable(
            name = "menu_item_modifier_groups",
            joinColumns = @JoinColumn(name = "menu_item_id"),
            inverseJoinColumns = @JoinColumn(name = "modifier_group_id")
    )
    @Builder.Default
    private Set<ModifierGroup> modifierGroups = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id", referencedColumnName = "id", insertable = false, updatable = false)
    @Builder.Default
    private Set<Media> medias = new HashSet<>();

    @Version
    private Long version;
}