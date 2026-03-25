package com.tuttime.payment.mapper;

import com.tuttime.payment.dto.RefundDto;
import com.tuttime.payment.entity.Refund;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface RefundMapper {

	RefundMapper INSTANCE = Mappers.getMapper(RefundMapper.class);

	@Mapping(source = "payment.id", target = "paymentId")
	RefundDto toDto(Refund refund);

	@Mapping(source = "paymentId", target = "payment.id")
	Refund toEntity(RefundDto refundDto);
}