package com.tuttime.user.converter;

import com.tuttime.user.entity.AuthProvider;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AuthProviderConverter implements AttributeConverter<AuthProvider, String> {
	@Override
	public String convertToDatabaseColumn(AuthProvider provider) {
		return provider == null ? null : provider.name();
	}

	@Override
	public AuthProvider convertToEntityAttribute(String dbData) {
		return dbData == null ? null : AuthProvider.valueOf(dbData);
	}
}