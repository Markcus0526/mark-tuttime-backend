package com.tuttime.analytics.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.tuttime.common.entity.BaseUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "system_events", schema = "analytics_service")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class SystemEvent extends BaseUuidEntity {

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "event_type", length = 100, nullable = false)
    private String eventType;

    @Column(name = "service_source", length = 50)
    private String serviceSource;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false)
    private JsonNode payload;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;
}