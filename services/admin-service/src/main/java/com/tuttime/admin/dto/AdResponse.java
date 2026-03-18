package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Response DTO for ad details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdResponse {
    
    private Long adId;
    private Long advertiserUserId;
    private String adTitle;
    private String adDescription;
    private String adType;
    private String contentUrl;
    private String externalLink;
    private String targetUrl;
    private BigDecimal paymentAmount;
    private BigDecimal burnPercentage;
    private BigDecimal burnedAmount;
    private Integer totalViews;
    private Integer totalClicks;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime startDate;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime endDate;
    private Boolean isHideable;
    private BigDecimal boostPerView;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime approvedAt;
    private Long approvedBy;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime updatedAt;
}

