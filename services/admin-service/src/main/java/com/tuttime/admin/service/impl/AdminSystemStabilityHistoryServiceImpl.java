package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.SystemStabilityHistoryResponse;
import com.tuttime.admin.service.AdminSystemStabilityHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin System Stability History Service Implementation
 * 
 * <p>Retrieves system stability history.
 * Note: Currently uses in-memory storage. For production, consider
 * persisting to database or integrating with monitoring systems.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSystemStabilityHistoryServiceImpl implements AdminSystemStabilityHistoryService {

    // In-memory storage for system stability history
    // In production, this should be replaced with database persistence
    private final Map<String, SystemStabilityHistoryResponse> historyStorage = new HashMap<>();

    @Override
    public Page<SystemStabilityHistoryResponse> getSystemStabilityHistory(
            Pageable pageable,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            String eventType,
            String serviceName) {
        
        log.debug("Getting system stability history: startDate={}, endDate={}, eventType={}, serviceName={}", 
                startDate, endDate, eventType, serviceName);
        
        List<SystemStabilityHistoryResponse> history = new ArrayList<>(historyStorage.values());
        
        // Apply filters
        history = history.stream()
                .filter(entry -> {
                    // Date filter
                    if (startDate != null && entry.getOccurredAt().isBefore(startDate)) {
                        return false;
                    }
                    if (endDate != null && entry.getOccurredAt().isAfter(endDate)) {
                        return false;
                    }
                    
                    // Event type filter
                    if (eventType != null && !eventType.isEmpty() && !eventType.equals(entry.getEventType())) {
                        return false;
                    }
                    
                    // Service name filter
                    if (serviceName != null && !serviceName.isEmpty() && !serviceName.equals(entry.getServiceName())) {
                        return false;
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
        
        // Sort by occurred date (newest first)
        history.sort((a, b) -> b.getOccurredAt().compareTo(a.getOccurredAt()));
        
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), history.size());
        List<SystemStabilityHistoryResponse> pageContent = start < history.size() 
                ? history.subList(start, end) 
                : new ArrayList<>();
        
        return new PageImpl<>(pageContent, pageable, history.size());
    }

    /**
     * Add a system stability event to history
     * This method can be called by monitoring systems or scheduled jobs
     */
    public void addStabilityEvent(SystemStabilityHistoryResponse event) {
        historyStorage.put(event.getHistoryId(), event);
        log.info("Added system stability event: {}", event.getHistoryId());
    }
}

