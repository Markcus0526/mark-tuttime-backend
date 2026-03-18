package com.tuttime.user.type;

import com.tuttime.user.entity.UserDevice;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;
import org.postgresql.util.PGobject;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

/**
 * Custom Hibernate UserType for PostgreSQL ENUM types.
 * 
 * <p>This type properly handles PostgreSQL enum types by using PGobject
 * to set the enum type name and value. This ensures PostgreSQL receives
 * the enum value in the correct format instead of VARCHAR.
 * 
 * <p><b>Usage:</b>
 * <pre>
 * {@code
 * @Type(PostgreSQLEnumType.class)
 * @Column(name = "device_type", nullable = false, columnDefinition = "device_type")
 * private DeviceType deviceType;
 * }
 * </pre>
 * 
 * <p><b>How it works:</b>
 * <ul>
 *   <li>When inserting: Converts Java enum to PGobject with type="device_type" and value="iOS"</li>
 *   <li>When reading: Extracts string value from ResultSet and converts to Java enum</li>
 *   <li>Works with schema-qualified types via search_path (user_service.device_type)</li>
 * </ul>
 * 
 * <p><b>Note:</b> The enum type name is extracted from the column definition
 * or can be explicitly set. For device_type enum, the type name is "device_type".
 * 
 * @see org.postgresql.util.PGobject
 */
@Slf4j
public class PostgreSQLEnumType implements UserType<Object> {
    
    /**
     * Returns SQL type code for PostgreSQL OTHER type.
     * This allows us to use PGobject for custom types.
     */
    @Override
    public int getSqlType() {
        return Types.OTHER;
    }
    
    /**
     * Returns the Java class that this type maps to.
     */
    @Override
    public Class<Object> returnedClass() {
        return Object.class;
    }
    
    /**
     * Checks if two values are equal.
     */
    @Override
    public boolean equals(Object x, Object y) {
        return Objects.equals(x, y);
    }
    
    /**
     * Returns hash code for the value.
     */
    @Override
    public int hashCode(Object x) {
        return x != null ? x.hashCode() : 0;
    }
    
    /**
     * Extracts enum value from ResultSet and converts to Java enum.
     * 
     * <p>This method reads the enum value as a string from the database
     * and converts it to the DeviceType enum. Since this UserType is
     * specifically for device_type enum, we can safely convert to DeviceType.
     * 
     * @param rs ResultSet containing the database value
     * @param position Column position (1-based)
     * @param session Hibernate session
     * @param owner Entity instance
     * @return DeviceType enum value or null
     */
    @Override
    public Object nullSafeGet(ResultSet rs, int position, 
                              SharedSessionContractImplementor session, 
                              Object owner) throws SQLException {
        String value = rs.getString(position);
        if (rs.wasNull() || value == null) {
            return null;
        }
        // Convert string to DeviceType enum
        try {
            return UserDevice.DeviceType.valueOf(value);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown device_type value '{}', defaulting to Web", value);
            return UserDevice.DeviceType.Web; // Default fallback
        }
    }
    
    /**
     * Converts Java enum to PostgreSQL enum using PGobject.
     * 
     * <p>This method creates a PGobject with:
     * <ul>
     *   <li>type: "device_type" (the PostgreSQL enum type name)</li>
     *   <li>value: "iOS", "Android", or "Web" (the enum value)</li>
     * </ul>
     * 
     * <p>PostgreSQL will automatically cast this to the enum type
     * based on the search_path and type name.
     * 
     * @param st PreparedStatement to set the value
     * @param value Java enum value (e.g., DeviceType.iOS)
     * @param index Parameter index (1-based)
     * @param session Hibernate session
     */
    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index,
                           SharedSessionContractImplementor session) throws SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
        } else {
            try {
                // Create PGobject to properly set PostgreSQL enum type
                PGobject pgObject = new PGobject();
                
                // Set the enum type name (must match the PostgreSQL enum type name)
                // For device_type enum in user_service schema, use "device_type"
                // PostgreSQL will resolve it via search_path (user_service, public)
                pgObject.setType("device_type");
                
                // Set the enum value (e.g., "iOS", "Android", "Web")
                // Convert enum to string using name() method
                String enumValue = value.toString();
                pgObject.setValue(enumValue);
                
                // Set the PGobject in the prepared statement
                st.setObject(index, pgObject);
                
                log.debug("Set PostgreSQL enum: type={}, value={}", pgObject.getType(), pgObject.getValue());
            } catch (SQLException e) {
                log.error("Error setting PostgreSQL enum value: {}", value, e);
                throw new SQLException("Failed to set PostgreSQL enum value: " + value, e);
            }
        }
    }
    
    /**
     * Returns a deep copy of the value.
     * For enums (immutable), we can return the same instance.
     */
    @Override
    public Object deepCopy(Object value) {
        return value; // Enums are immutable
    }
    
    /**
     * Returns whether this type is mutable.
     * Enums are immutable, so return false.
     */
    @Override
    public boolean isMutable() {
        return false;
    }
    
    /**
     * Disassembles the value for caching.
     */
    @Override
    public Serializable disassemble(Object value) {
        return (Serializable) value;
    }
    
    /**
     * Assembles the value from cache.
     */
    @Override
    public Object assemble(Serializable cached, Object owner) {
        return cached;
    }
    
    /**
     * Replaces the detached value with the managed value.
     */
    @Override
    public Object replace(Object detached, Object managed, Object owner) {
        return detached;
    }
}

