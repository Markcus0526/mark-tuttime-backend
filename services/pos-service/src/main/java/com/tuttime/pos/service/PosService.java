package com.tuttime.pos.service;

import com.tuttime.pos.dto.PosConfigDto;
import com.tuttime.pos.dto.PosSyncDto;
import com.tuttime.pos.dto.PosSyncLogDto;
import com.tuttime.pos.entity.PosConfig;

import java.util.UUID;

public interface PosService {

    // Manual/On-demand Sync
    void syncBooking(PosSyncDto.BookingSyncRequest request);

    void syncOrder(PosSyncDto.OrderSyncRequest request);

    void syncMenu(PosSyncDto.MenuSyncRequest request);

    // Maintenance & Recovery
    void forceSyncByBranchId(UUID branchId);

    PosSyncLogDto.PosSyncLogResponse getSyncLogs(UUID entityId);

    // Configuration
    PosConfig saveConfig(PosConfigDto.PosConfigRequest request);

    void saveLog(UUID branchId, UUID entityId, String entityType, String action, Object request, String response, String status);
}
