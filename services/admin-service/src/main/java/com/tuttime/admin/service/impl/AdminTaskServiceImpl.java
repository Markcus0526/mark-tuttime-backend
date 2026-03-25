package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.TaskRequest;
import com.tuttime.admin.dto.TaskResponse;
import com.tuttime.admin.rest.client.ContentServiceRestClient;
import com.tuttime.admin.service.AdminTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Admin Task Service Implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTaskServiceImpl implements AdminTaskService {

	private final ContentServiceRestClient contentServiceRestClient;

	@Override
	public Page<TaskResponse> listTasks(Pageable pageable, String status) {
		return contentServiceRestClient.listTasks(pageable, status);
	}

	@Override
	public TaskResponse getTask(Long id) {
		return contentServiceRestClient.getTask(id);
	}

	@Override
	public TaskResponse createTask(TaskRequest request, Long adminId) {
		return contentServiceRestClient.createTask(request, adminId);
	}

	@Override
	public TaskResponse updateTask(Long id, TaskRequest request) {
		return contentServiceRestClient.updateTask(id, request);
	}

	@Override
	public void deleteTask(Long id) {
		contentServiceRestClient.deleteTask(id);
	}

	@Override
	public TaskResponse activateTask(Long id) {
		return contentServiceRestClient.activateTask(id);
	}

	@Override
	public TaskResponse deactivateTask(Long id) {
		return contentServiceRestClient.deactivateTask(id);
	}

	@Override
	public java.util.Map<String, Object> getDailyWatchVideoStatistics(java.time.OffsetDateTime date) {
		return contentServiceRestClient.getDailyWatchVideoStatistics(date);
	}

	@Override
	public java.util.Map<String, Object> getWatchVideoTaskStatistics(Long taskId) {
		return contentServiceRestClient.getWatchVideoTaskStatistics(taskId);
	}

	@Override
	public java.util.Map<String, Object> getUserWatchVideoStatistics(Long userId) {
		return contentServiceRestClient.getUserWatchVideoStatistics(userId);
	}

	@Override
	public java.util.List<java.util.Map<String, Object>> getDateRangeWatchVideoStatistics(
			java.time.OffsetDateTime startDate, java.time.OffsetDateTime endDate) {
		return contentServiceRestClient.getDateRangeWatchVideoStatistics(startDate, endDate);
	}

	@Override
	public java.util.Map<String, Object> backfillExistingUsersWatchVideoTasks() {
		return contentServiceRestClient.backfillExistingUsersWatchVideoTasks();
	}
}

