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
@EqualsAndHashCode
public class UserBookmarkId implements Serializable {
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "branch_id")
    private UUID branchId;
}