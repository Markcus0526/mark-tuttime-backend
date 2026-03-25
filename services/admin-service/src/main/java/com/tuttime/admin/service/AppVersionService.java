package com.tuttime.admin.service;

import com.tuttime.admin.dto.AppVersionResponse;
import com.tuttime.admin.dto.UpdateAppVersionRequest;

/**
 * App Version Service Interface
 */
public interface AppVersionService {
	/**
	 * Get the active app version configuration
	 *
	 * @return AppVersionResponse containing version info
	 */
	AppVersionResponse getAppVersion();

	/**
	 * Update app version configuration
	 * Deactivates the old version and creates a new active one
	 *
	 * @param request   Update request with new version info
	 * @param updatedBy Admin user ID who is updating
	 * @return Updated AppVersionResponse
	 */
	AppVersionResponse updateAppVersion(UpdateAppVersionRequest request, Long updatedBy);
}

