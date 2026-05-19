package com.tuttime.menu.entity;

import com.tuttime.common.entity.BaseUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "menu_tags", schema = "menu_service")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MenuTag extends BaseUuidEntity {

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "icon_url", length = 255)
    private String iconUrl;

}

