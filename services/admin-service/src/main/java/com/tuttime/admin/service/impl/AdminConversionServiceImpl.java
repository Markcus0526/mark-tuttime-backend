package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.ConversionInfoResponse;
import com.tuttime.admin.dto.ConversionLedgerEntry;
import com.tuttime.admin.dto.ConversionResponse;
import com.tuttime.admin.grpc.client.ConversionServiceGrpcClient;
import com.tuttime.admin.grpc.client.UserServiceGrpcClient;
import com.tuttime.admin.service.AdminConversionService;
import com.tuttime.common.proto.ConversionTask;
import com.tuttime.common.proto.GetConversionInfoResponse;
import com.tuttime.common.proto.GetMonthlyConversionLedgerResponse;
import com.tuttime.common.proto.GetUserConversionsResponse;
import com.tuttime.common.proto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin Conversion Service Implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminConversionServiceImpl implements AdminConversionService {

    private final ConversionServiceGrpcClient conversionServiceGrpcClient;
    private final UserServiceGrpcClient userServiceGrpcClient;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public Page<ConversionResponse> getUserConversions(Long userId, Pageable pageable) {
        // Conversion-service not deployed - return empty page immediately
        log.debug("Conversion-service not deployed - returning empty conversions page for user: {}", userId);
        return new PageImpl<>(
                List.of(),
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()),
                0L
        );
    }

    @Override
    public ConversionInfoResponse getConversionInfo(Long userId) {
        // Conversion-service not deployed - return default values immediately
        log.debug("Conversion-service not deployed - returning default conversion info for user: {}", userId);
        return ConversionInfoResponse.builder()
                .userId(userId)
                .lastMonthApartConversion(BigDecimal.ZERO)
                .nextConversionTime(null)
                .thisMonthForecastAmount(BigDecimal.ZERO)
                .build();
    }

    private ConversionResponse mapConversionTask(ConversionTask task) {
        return ConversionResponse.builder()
                .conversionId(task.getConversionId())
                .userId(task.getUserId())
                .conversionDate(parseDate(task.getConversionDate()))
                .apCredits(new BigDecimal(task.getApCredits()))
                .tokens(new BigDecimal(task.getTokens()))
                .status(task.getStatus())
                .completedAt(parseDateTime(task.getCompletedAt()))
                .build();
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("Failed to parse date: {}", dateStr, e);
            return null;
        }
    }

    /**
     * Parses a datetime string to OffsetDateTime.
     * 
     * <p><b>Timezone Note:</b> Parsed dates are assumed to be in UTC timezone.
     * All API responses serialize dates in UTC, so this is safe.
     * 
     * @param dateTimeStr The datetime string to parse (ISO format, UTC)
     * @return Parsed OffsetDateTime in UTC, or null if parsing fails
     */
    private OffsetDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            // Try parsing as OffsetDateTime first
            return OffsetDateTime.parse(dateTimeStr).withOffsetSameInstant(ZoneOffset.UTC);
        } catch (Exception e) {
            log.warn("Failed to parse datetime: {}", dateTimeStr, e);
            return null;
        }
    }

    @Override
    public Page<ConversionLedgerEntry> getMonthlyConversionLedger(int year, int month, org.springframework.data.domain.Pageable pageable) {
        // Conversion-service not deployed - return empty page immediately
        log.debug("Conversion-service not deployed - returning empty conversion ledger for year={}, month={}", year, month);
        return new PageImpl<>(
                List.of(),
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()),
                0L
        );
    }
}

