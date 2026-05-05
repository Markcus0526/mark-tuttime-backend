package com.tuttime.restaurant.entity;

import com.tuttime.common.entity.BaseUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.apache.kafka.common.message.OffsetCommitRequestData;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tags", schema = "restaurant_service")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Tag extends BaseUuidEntity {

	@Column(name = "name", nullable = false, unique = true, length = 50)
	private String name;

	@Column(name = "icon_url", length = 255)
	private String iconUrl;

	@Column(name = "category", nullable = false, length = 30)
	private String category; // e.g., 'CUISINE', 'DIETARY'
}

