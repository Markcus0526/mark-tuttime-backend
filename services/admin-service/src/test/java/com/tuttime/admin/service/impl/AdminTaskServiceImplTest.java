package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.TaskRequest;
import com.tuttime.admin.dto.TaskResponse;
import com.tuttime.admin.rest.client.ContentServiceRestClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminTaskServiceImpl Tests")
class AdminTaskServiceImplTest {

	@Mock
	private ContentServiceRestClient contentServiceRestClient;

	@InjectMocks
	private AdminTaskServiceImpl adminTaskService;

	@Test
	@DisplayName("Should list tasks successfully")
	void testListTasks_Success_ReturnsPage() {
		// Given
		PageRequest pageable = PageRequest.of(0, 10);
		TaskResponse response = TaskResponse.builder()
				.taskId(1L)
				.taskTitle("Daily Login")
				.taskType("DAILY")
				.status("ACTIVE")
				.build();

		Page<TaskResponse> page = new PageImpl<>(Arrays.asList(response), pageable, 1);
		when(contentServiceRestClient.listTasks(pageable, "ACTIVE")).thenReturn(page);

		// When
		Page<TaskResponse> result = adminTaskService.listTasks(pageable, "ACTIVE");

		// Then
		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent()).hasSize(1);
	}

	@Test
	@DisplayName("Should get task by ID successfully")
	void testGetTask_Success_ReturnsTask() {
		// Given
		Long id = 1L;
		TaskResponse response = TaskResponse.builder()
				.taskId(id)
				.taskTitle("Daily Login")
				.build();

		when(contentServiceRestClient.getTask(id)).thenReturn(response);

		// When
		TaskResponse result = adminTaskService.getTask(id);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getTaskId()).isEqualTo(id);
	}

	@Test
	@DisplayName("Should create task successfully")
	void testCreateTask_Success() {
		// Given
		TaskRequest request = TaskRequest.builder()
				.taskTitle("Daily Login")
				.taskType("DAILY")
				.bonusAp(new BigDecimal("2.0"))
				.build();

		TaskResponse response = TaskResponse.builder()
				.taskId(1L)
				.taskTitle("Daily Login")
				.build();

		when(contentServiceRestClient.createTask(request, 100L)).thenReturn(response);

		// When
		TaskResponse result = adminTaskService.createTask(request, 100L);

		// Then
		assertThat(result).isNotNull();
		verify(contentServiceRestClient).createTask(request, 100L);
	}

	@Test
	@DisplayName("Should update task successfully")
	void testUpdateTask_Success() {
		// Given
		Long id = 1L;
		TaskRequest request = TaskRequest.builder()
				.taskTitle("Updated Title")
				.build();

		TaskResponse response = TaskResponse.builder()
				.taskId(id)
				.taskTitle("Updated Title")
				.build();

		when(contentServiceRestClient.updateTask(id, request)).thenReturn(response);

		// When
		TaskResponse result = adminTaskService.updateTask(id, request);

		// Then
		assertThat(result).isNotNull();
		verify(contentServiceRestClient).updateTask(id, request);
	}

	@Test
	@DisplayName("Should delete task successfully")
	void testDeleteTask_Success() {
		// Given
		Long id = 1L;
		doNothing().when(contentServiceRestClient).deleteTask(id);

		// When
		adminTaskService.deleteTask(id);

		// Then
		verify(contentServiceRestClient).deleteTask(id);
	}

	@Test
	@DisplayName("Should activate task successfully")
	void testActivateTask_Success() {
		// Given
		Long id = 1L;
		TaskResponse response = TaskResponse.builder()
				.taskId(id)
				.status("ACTIVE")
				.build();

		when(contentServiceRestClient.activateTask(id)).thenReturn(response);

		// When
		TaskResponse result = adminTaskService.activateTask(id);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getStatus()).isEqualTo("ACTIVE");
	}

	@Test
	@DisplayName("Should deactivate task successfully")
	void testDeactivateTask_Success() {
		// Given
		Long id = 1L;
		TaskResponse response = TaskResponse.builder()
				.taskId(id)
				.status("INACTIVE")
				.build();

		when(contentServiceRestClient.deactivateTask(id)).thenReturn(response);

		// When
		TaskResponse result = adminTaskService.deactivateTask(id);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getStatus()).isEqualTo("INACTIVE");
	}
}

