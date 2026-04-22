package com.tuttime.user.converter;

import com.tuttime.common.enums.UserStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UserStatusConverter implements AttributeConverter<UserStatus, String> {
	@Override
	public String convertToDatabaseColumn(UserStatus status) {
		return status == null ? null : status.name();
	}

	@Override
	public UserStatus convertToEntityAttribute(String dbData) {
		return dbData == null ? null : UserStatus.valueOf(dbData);
	}
}