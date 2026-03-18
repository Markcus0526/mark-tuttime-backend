package com.tuttime.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * User Referrals Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReferralsResponse {
    
    private Long userId;
    private String username;
    private String referralCode;
    private Integer totalReferrals;
    private Integer activeReferrals;
    private List<ReferralInfo> referrals;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReferralInfo {
        private Long userId;
        private String email;
        private String phone;
        private String firstName;
        private String lastName;
        private Boolean isActive;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
        private OffsetDateTime referredAt;
    }
}

