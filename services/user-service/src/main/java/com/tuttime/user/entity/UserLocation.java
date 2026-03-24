package com.tuttime.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_locations", schema = "user_service",
        indexes = @Index(name = "idx_user_locations_gist", columnList = "location"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLocation {
    @Id
    @GeneratedValue(generator = "gen_random_uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "geography(Point,4326)")
    private Point location;

    @Column(name = "accuracy_meters")
    private Float accuracyMeters;

    @Builder.Default
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt = Instant.now();
}
