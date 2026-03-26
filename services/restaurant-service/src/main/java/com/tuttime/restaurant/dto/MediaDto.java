package com.tuttime.restaurant.dto;

import java.util.UUID;

import com.tuttime.restaurant.entity.EntityType;
import com.tuttime.restaurant.entity.MediaType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

}
