package com.tuttime.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class OrderTableId implements Serializable {

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "table_id")
    private UUID tableId;
}