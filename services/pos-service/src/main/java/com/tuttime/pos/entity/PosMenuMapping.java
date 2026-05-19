package com.tuttime.pos.entity;

import com.tuttime.common.entity.BaseSerialEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pos_menu_mappings", schema = "pos_service")
@Getter
@Setter
public class PosMenuMapping extends BaseSerialEntity {

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "menu_item_id", nullable = false)
    private UUID menuItemId;

    @Column(name = "external_pos_id", nullable = false)
    private String externalPosId;

    @Column(name = "external_name")
    private String externalName;

    @Column(name = "external_price")
    private BigDecimal externalPrice;

    @Column(name = "last_synced_at")
    private OffsetDateTime lastSyncedAt = OffsetDateTime.now();
}
