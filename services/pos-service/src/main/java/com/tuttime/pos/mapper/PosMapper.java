package com.tuttime.pos.mapper;

import com.tuttime.pos.dto.PosSyncLogResponse;
import com.tuttime.pos.dto.SyncLogDetail;
import com.tuttime.pos.entity.PosLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PosMapper {

	@Mapping(target = "entityId", source = "entityId")
	@Mapping(target = "logs", source = "logs")
	PosSyncLogResponse toResponse(UUID entityId, List<PosLog> logs);

}
