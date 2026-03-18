package com.tuttime.user.converter;

import com.tuttime.user.entity.UserDevice.DeviceType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converter for DeviceType enum to PostgreSQL enum type.
 *
 * <p>This converter handles the mapping between Java DeviceType enum
 * and PostgreSQL device_type enum type.
 */
@Converter(autoApply = true)
public class DeviceTypeConverter implements AttributeConverter<DeviceType, String> {

    @Override
    public String convertToDatabaseColumn(DeviceType attribute) {
        // Column is NOT NULL, so we must return a valid enum value
        // Default to Web if null to prevent database constraint violations
        if (attribute == null) {
            return DeviceType.Web.name();
        }
        return attribute.name();
    }

    @Override
    public DeviceType convertToEntityAttribute(String dbData) {
        // Column is NOT NULL, but handle null/empty gracefully
        if (dbData == null || dbData.trim().isEmpty()) {
            return DeviceType.Web; // Default to Web for null/empty
        }
        
        // Normalize the input - trim whitespace and handle case variations
        String normalized = dbData.trim();
        
        // Try exact match first (case-sensitive)
        try {
            return DeviceType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Handle case-insensitive matching and variations
            String lower = normalized.toLowerCase();
            
            // Handle iOS variations
            if (lower.equals("ios") || lower.equals("iphone") || lower.equals("ipad") || 
                lower.equals("ipod") || lower.contains("ios")) {
                return DeviceType.iOS;
            }
            
            // Handle Android variations
            if (lower.equals("android") || lower.contains("android")) {
                return DeviceType.Android;
            }
            
            // Handle Web variations
            if (lower.equals("web") || lower.equals("desktop") || lower.equals("windows") || 
                lower.equals("mac") || lower.equals("linux") || lower.equals("unknown") ||
                lower.equals("emulator") || lower.equals("simulator")) {
                return DeviceType.Web;
            }
            
            // Default to Web for any unknown values
            return DeviceType.Web;
        }
    }
}

