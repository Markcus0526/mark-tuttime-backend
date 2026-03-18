package com.tuttime.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for phone number validation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneNumberRequest {
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;
    
    /**
     * Device type: "Android", "iOS", or "Web" (optional, fallback only)
     * 
     * <p><b>Recommendation:</b> For mobile apps, set the {@code X-Device-Type} HTTP header instead.
     * You do NOT need to include this field in the request body if the header is set.
     * 
     * <p><b>Note:</b> The device type is primarily read from the HTTP request header 
     * {@code X-Device-Type}. This field is only used as a fallback if the header is not present.
     * 
     * <p>Used to determine verification method:
     * <ul>
     *   <li>Android → Flash Call</li>
     *   <li>iOS → SMS</li>
     *   <li>Web/Other → SMS</li>
     * </ul>
     */
    private String deviceType;
}

