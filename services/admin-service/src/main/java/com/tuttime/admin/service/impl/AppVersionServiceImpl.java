package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.AppVersionResponse;
import com.tuttime.admin.dto.UpdateAppVersionRequest;
import com.tuttime.admin.entity.AppVersion;
import com.tuttime.admin.repository.AppVersionRepository;
import com.tuttime.admin.service.AppVersionService;
import com.tuttime.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * App Version Service Implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppVersionServiceImpl implements AppVersionService {

    private final AppVersionRepository appVersionRepository;
    
    // URL validation pattern (basic validation)
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^https?://[\\w\\-]+(\\.[\\w\\-]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?$",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    @Transactional(readOnly = true)
    public AppVersionResponse getAppVersion() {
        log.debug("Getting active app version");
        
        AppVersion appVersion = appVersionRepository.findByIsActiveTrue()
                .orElseThrow(() -> new BusinessException("No active app version found"));
        
        return mapToResponse(appVersion);
    }

    @Override
    @Transactional(
        isolation = Isolation.SERIALIZABLE,
        rollbackFor = Exception.class
    )
    public AppVersionResponse updateAppVersion(UpdateAppVersionRequest request, Long updatedBy) {
        log.info("Updating app version - Android: {}, iOS: {}, Updated by: {}", 
                request.getAndroidVersion(), request.getIosVersion(), updatedBy);
        
        // Validate URLs before starting transaction
        validateUrl(request.getAndroidInstallLink(), "Android install link");
        validateUrl(request.getIosInstallLink(), "iOS install link");
        
        try {
            // Find and lock current active version to prevent concurrent updates
            // Using pessimistic lock ensures only one update can proceed at a time
            AppVersion currentVersion = appVersionRepository.findByIsActiveTrueWithLock()
                    .orElse(null);
            
            if (currentVersion != null) {
                log.debug("Deactivating current app version: {}", currentVersion.getVersionId());
                currentVersion.setIsActive(false);
                // Use saveAndFlush to ensure deactivation is persisted before creating new version
                // This ensures the unique constraint is released before we try to create the new one
                appVersionRepository.saveAndFlush(currentVersion);
                log.debug("Deactivated previous app version: {}", currentVersion.getVersionId());
            } else {
                log.warn("No active app version found to deactivate - this may be the first version");
            }
            
            // Create new active version
            AppVersion newVersion = AppVersion.builder()
                    .androidVersion(request.getAndroidVersion())
                    .androidInstallLink(request.getAndroidInstallLink())
                    .iosVersion(request.getIosVersion())
                    .iosInstallLink(request.getIosInstallLink())
                    .isActive(true)
                    .updatedBy(updatedBy)
                    .build();
            
            AppVersion saved = appVersionRepository.save(newVersion);
            // Flush to ensure new version is persisted and constraint is checked immediately
            appVersionRepository.flush();
            
            log.info("Successfully created new app version: {}", saved.getVersionId());
            
            return mapToResponse(saved);
            
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while updating app version: {}", e.getMessage(), e);
            // Check if it's a unique constraint violation on the active version
            String errorMessage = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (errorMessage.contains("uk_app_version_active") || 
                errorMessage.contains("unique constraint") ||
                errorMessage.contains("duplicate key")) {
                throw new BusinessException(
                    "Failed to update app version: Another update may be in progress. Please try again in a moment."
                );
            }
            throw new BusinessException("Failed to update app version due to data integrity violation: " + 
                    (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        } catch (JpaSystemException e) {
            log.error("JPA system error while updating app version: {}", e.getMessage(), e);
            // Check if it's related to constraint violation
            if (e.getCause() instanceof DataIntegrityViolationException) {
                DataIntegrityViolationException cause = (DataIntegrityViolationException) e.getCause();
                String errorMessage = cause.getMessage() != null ? cause.getMessage().toLowerCase() : "";
                if (errorMessage.contains("uk_app_version_active") || 
                    errorMessage.contains("unique constraint")) {
                    throw new BusinessException(
                        "Failed to update app version: Another update may be in progress. Please try again in a moment."
                    );
                }
            }
            throw new BusinessException("Failed to update app version: " + 
                    (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error while updating app version: {}", e.getMessage(), e);
            // Transaction will rollback automatically due to @Transactional(rollbackFor = Exception.class)
            throw new BusinessException("Failed to update app version: " + 
                    (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }
    }
    
    private AppVersionResponse mapToResponse(AppVersion appVersion) {
        return AppVersionResponse.builder()
                .androidVersion(appVersion.getAndroidVersion())
                .androidInstallLink(appVersion.getAndroidInstallLink())
                .iosVersion(appVersion.getIosVersion())
                .iosInstallLink(appVersion.getIosInstallLink())
                .updatedAt(appVersion.getUpdatedAt())
                .updatedBy(appVersion.getUpdatedBy())
                .build();
    }
    
    private void validateUrl(String url, String fieldName) {
        if (url == null || url.trim().isEmpty()) {
            throw new BusinessException(fieldName + " cannot be empty");
        }
        
        if (!URL_PATTERN.matcher(url.trim()).matches()) {
            throw new BusinessException(fieldName + " must be a valid URL");
        }
    }
}

