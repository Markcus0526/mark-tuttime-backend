package com.tuttime.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Language Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LanguageRequest {

    @NotBlank(message = "Language code is required")
    @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$", message = "Language code must be in format 'xx' or 'xx-XX' (e.g., 'en', 'ko', 'en-US')")
    @Size(min = 2, max = 10, message = "Language code must be between 2 and 10 characters")
    private String languageCode;

    @NotBlank(message = "Language name is required")
    @Size(max = 100, message = "Language name must not exceed 100 characters")
    private String languageName;

    @Size(max = 100, message = "Native name must not exceed 100 characters")
    private String nativeName;

    private Boolean isActive;

    private Boolean isDefault;

    private Integer displayOrder;
}

