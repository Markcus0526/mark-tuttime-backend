package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for sending push notifications to all never-started users
 * All fields are optional - defaults will be used if not provided
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendPushToNeverStartedUsersRequest {

	/**
	 * Notification title (optional)
	 * Defaults to "Start Mining Now" if not provided
	 */
	private String title;

	/**
	 * Notification body (optional)
	 * Defaults to "You haven't started mining yet. Start mining and earn AP!" if not provided
	 */
	private String body;

	/**
	 * Additional data payload (optional)
	 */
	private Map<String, String> data;

	/**
	 * Notification priority (optional)
	 * Defaults to NORMAL if not specified
	 */
	private String priority;
}

