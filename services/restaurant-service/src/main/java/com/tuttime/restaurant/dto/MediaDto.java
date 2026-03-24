package com.tuttime.restaurant.dto;

import com.tuttime.restaurant.entity.EntityType;
import com.tuttime.restaurant.entity.MediaType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaDto {

    private Long mediaId;
    private EntityType entityType;
    private Long entityId;
    private String url;
    private String altText;
    private Boolean isPrimary;
    private MediaType mediaType;

}
