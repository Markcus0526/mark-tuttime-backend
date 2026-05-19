package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.ScheduleDto;
import com.tuttime.restaurant.entity.Schedule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ScheduleMapper {

    @Mapping(source = "id", target = "scheduleId")
    ScheduleDto.ScheduleResponse toResponse(Schedule entity);

    Schedule toEntity(ScheduleDto.ScheduleRequest request);

    void updateEntityFromRequest(ScheduleDto.ScheduleRequest request, @MappingTarget Schedule entity);

}