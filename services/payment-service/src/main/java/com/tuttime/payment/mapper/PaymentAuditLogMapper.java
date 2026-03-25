package com.tuttime.payment.mapper;

import com.tuttime.payment.dto.PaymentAuditLogDto;
import com.tuttime.payment.entity.PaymentAuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface PaymentAuditLogMapper {

	PaymentAuditLogMapper INSTANCE = Mappers.getMapper(PaymentAuditLogMapper.class);

	@Mapping(source = "payment.id", target = "paymentId")
	PaymentAuditLogDto toDto(PaymentAuditLog auditLog);

	@Mapping(source = "paymentId", target = "payment.id")
	PaymentAuditLog toEntity(PaymentAuditLogDto auditLogDto);
}
