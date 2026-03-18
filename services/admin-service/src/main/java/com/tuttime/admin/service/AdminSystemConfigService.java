package com.tuttime.admin.service;

import com.tuttime.admin.dto.StatusLevelRequest;
import com.tuttime.admin.dto.StatusLevelResponse;
import com.tuttime.admin.dto.SystemConfigResponse;
import com.tuttime.admin.dto.UpdateSystemConfigRequest;

import java.util.List;

/**
 * Admin System Config Service Interface
 */
public interface AdminSystemConfigService {
    SystemConfigResponse getSystemConfig();
    SystemConfigResponse updateSystemConfig(UpdateSystemConfigRequest request, Long updatedBy);
    
    // Status Level Management
    List<StatusLevelResponse> getAllStatusLevels();
    StatusLevelResponse getStatusLevelByLevelName(String levelName);
    StatusLevelResponse createStatusLevel(StatusLevelRequest request, Long createdBy);
    StatusLevelResponse updateStatusLevel(String levelName, StatusLevelRequest request);
    void deleteStatusLevel(String levelName);
}

