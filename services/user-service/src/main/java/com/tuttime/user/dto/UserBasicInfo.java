package com.tuttime.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Basic user information DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBasicInfo {
    private String firstName;
    private String lastName;
    private String email;
    private String username;
    private String phone;
    private String telegram;
    private String instagram;
    private String country;
    private String city;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime registrationDate;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime lastActive;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime lastLogin;
    private List<DeviceInfo> devices;
}

