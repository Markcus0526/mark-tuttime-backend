package com.tuttime.restaurant.dto;

import com.tuttime.restaurant.enums.EntityType;
import com.tuttime.restaurant.enums.MediaType;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaDto {

	private UUID mediaId;

	private EntityType entityType;

	private UUID entityId;

	private String url;

	private String altText;

	private Boolean isPrimary;

	private MediaType mediaType;

	private Instant createdAt;

	private Instant updatedAt;
}
