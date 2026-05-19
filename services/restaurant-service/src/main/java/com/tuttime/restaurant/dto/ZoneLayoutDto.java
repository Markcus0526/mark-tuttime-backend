package com.tuttime.restaurant.dto;

import java.util.List;
import java.util.UUID;

public class ZoneLayoutDto {

    public record LayoutDto(
            CanvasDto canvas,
            List<TableDto> tables,
            List<FixtureDto> fixtures
    ) {
    }

    public record CanvasDto(
            Integer width,
            Integer height
    ) {
    }

    public record FixtureDto(
            UUID id,
            String kind,
            Double x,
            Double y,
            Integer rotation,
            String label
    ) {
    }

    public record TableDto(
            UUID id,
            String type,
            Double x,
            Double y,
            String status,
            String label,
            String notes,
            Integer seats
    ) {
    }

}
