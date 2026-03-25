package com.tuttime.payment.mapper;

import com.tuttime.payment.dto.PaymentDto;
import com.tuttime.payment.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", uses = {RefundMapper.class, PaymentAuditLogMapper.class})
public interface PaymentMapper {

	PaymentMapper INSTANCE = Mappers.getMapper(PaymentMapper.class);

	PaymentDto toDto(Payment payment);

	Payment toEntity(PaymentDto paymentDto);
}