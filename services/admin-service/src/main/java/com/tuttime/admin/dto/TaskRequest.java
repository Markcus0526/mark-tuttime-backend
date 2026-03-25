package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Request DTO for creating/updating tasks
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequest {

	@NotBlank(message = "Task title is required")
	private String taskTitle;

	private String taskDescription;

	@NotBlank(message = "Task type is required")
	private String taskType; // DAILY, WEEKLY, SPECIAL, REFERRAL

	@NotNull(message = "Bonus AP is required")
	private BigDecimal bonusAp; // Mining speed bonus in AP/hour when task is completed

	private String status; // INACTIVE, ACTIVE

	private Integer maxCompletions; // null = unlimited

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime startDate;

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime endDate;

	private Boolean isRepeatable;

	private Integer repeatIntervalDays;

	// Action type: WATCH_VIDEO, APART_SHARING_TASK, DAILY_TASK
	private String actionType;

	private Map<String, Object> metadata;

	// Builder method for creating instances
	public static TaskRequestBuilder builder() {
		return new TaskRequestBuilder();
	}

	// Manual builder implementation to avoid Lombok annotation processing issues
	public static class TaskRequestBuilder {
		private String taskTitle;
		private String taskDescription;
		private String taskType;
		private BigDecimal bonusAp;
		private String status;
		private Integer maxCompletions;
		private OffsetDateTime startDate;
		private OffsetDateTime endDate;
		private Boolean isRepeatable;
		private Integer repeatIntervalDays;
		private String actionType;
		private Map<String, Object> metadata;

		public TaskRequestBuilder taskTitle(String taskTitle) {
			this.taskTitle = taskTitle;
			return this;
		}

		public TaskRequestBuilder taskDescription(String taskDescription) {
			this.taskDescription = taskDescription;
			return this;
		}

		public TaskRequestBuilder taskType(String taskType) {
			this.taskType = taskType;
			return this;
		}

		public TaskRequestBuilder bonusAp(BigDecimal bonusAp) {
			this.bonusAp = bonusAp;
			return this;
		}

		public TaskRequestBuilder status(String status) {
			this.status = status;
			return this;
		}

		public TaskRequestBuilder maxCompletions(Integer maxCompletions) {
			this.maxCompletions = maxCompletions;
			return this;
		}

		public TaskRequestBuilder startDate(OffsetDateTime startDate) {
			this.startDate = startDate;
			return this;
		}

		public TaskRequestBuilder endDate(OffsetDateTime endDate) {
			this.endDate = endDate;
			return this;
		}

		public TaskRequestBuilder isRepeatable(Boolean isRepeatable) {
			this.isRepeatable = isRepeatable;
			return this;
		}

		public TaskRequestBuilder repeatIntervalDays(Integer repeatIntervalDays) {
			this.repeatIntervalDays = repeatIntervalDays;
			return this;
		}

		public TaskRequestBuilder actionType(String actionType) {
			this.actionType = actionType;
			return this;
		}

		public TaskRequestBuilder metadata(Map<String, Object> metadata) {
			this.metadata = metadata;
			return this;
		}

		public TaskRequest build() {
			return new TaskRequest(
					taskTitle, taskDescription, taskType, bonusAp,
					status, maxCompletions, startDate, endDate, isRepeatable,
					repeatIntervalDays, actionType, metadata
			);
		}
	}
}

