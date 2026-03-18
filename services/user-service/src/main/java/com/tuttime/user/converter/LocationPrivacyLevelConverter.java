package com.tuttime.user.converter;

import com.tuttime.user.entity.LocationPrivacyLevel;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter for LocationPrivacyLevel enum.
 * 
 * Converts enum to lowercase string for database storage (to match database constraint)
 * and converts back to enum when reading from database.
 */
@Converter(autoApply = true)
public class LocationPrivacyLevelConverter implements AttributeConverter<LocationPrivacyLevel, String> {

    @Override
    public String convertToDatabaseColumn(LocationPrivacyLevel attribute) {
        if (attribute == null) {
            return null;
        }
        // Convert enum name to lowercase for database storage
        return attribute.name().toLowerCase();
    }

    @Override
    public LocationPrivacyLevel convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        String upperCase = dbData.trim().toUpperCase();
        
        // Migration: Convert deprecated CITY and COUNTRY to EXACT (auto-detect mode)
        if ("CITY".equals(upperCase) || "COUNTRY".equals(upperCase)) {
            return LocationPrivacyLevel.EXACT;
        }
        
        // Convert database string (lowercase) back to enum (uppercase)
        try {
            return LocationPrivacyLevel.valueOf(upperCase);
        } catch (IllegalArgumentException e) {
            // If invalid value, return default
            return LocationPrivacyLevel.EXACT;
        }
    }
}

