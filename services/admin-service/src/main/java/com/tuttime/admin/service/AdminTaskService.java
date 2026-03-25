package com.tuttime.admin.service;

import com.tuttime.admin.dto.TaskRequest;
import com.tuttime.admin.dto.TaskResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Admin Task Service Interface
 */
public interface AdminTaskService {
	Page<TaskResponse> listTasks(Pageable pageable, String status);

	TaskResponse getTask(Long id);

	TaskResponse createTask(TaskRequest request, Long adminId);

	TaskResponse updateTask(Long id, TaskRequest request);

	void deleteTask(Long id);

	TaskResponse activateTask(Long id);

	TaskResponse deactivateTask(Long id);

	// Watch Video Task Statistics
	java.util.Map<String, Object> getDailyWatchVideoStatistics(java.time.OffsetDateTime date);

	java.util.Map<String, Object> getWatchVideoTaskStatistics(Long taskId);

	java.util.Map<String, Object> getUserWatchVideoStatistics(Long userId);

	java.util.List<java.util.Map<String, Object>> getDateRangeWatchVideoStatistics(
			java.time.OffsetDateTime startDate, java.time.OffsetDateTime endDate);

	java.util.Map<String, Object> backfillExistingUsersWatchVideoTasks();
}

