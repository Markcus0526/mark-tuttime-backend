package com.tuttime.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating notification campaign
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCampaignRequest {

	@NotBlank(message = "Campaign name is required")
	private String campaignName;

	private String templateKey; // Optional

	@NotBlank(message = "Source title is required")
	private String sourceTitle; // Deprecated: use sourceTitleEn instead

	@NotBlank(message = "Source message is required")
	private String sourceMessage; // Deprecated: use sourceMessageEn instead

	@NotBlank(message = "English source title is required")
	private String sourceTitleEn;

	@NotBlank(message = "English source message is required")
	private String sourceMessageEn;

	@NotBlank(message = "Russian source title is required")
	private String sourceTitleRu;

	@NotBlank(message = "Russian source message is required")
	private String sourceMessageRu;

	@Builder.Default
	private String sourceLanguage = "en";

	@NotNull(message = "Channels are required")
	private List<String> channels; // ['PUSH', 'EMAIL', 'IN_APP', 'SMS']

	@NotBlank(message = "Target audience is required")
	private String targetAudience; // 'INACTIVE_MINING', 'NEVER_STARTED_MINING', 'USER_GROUP', 'ALL_USERS', 'CUSTOM_CRITERIA'

	private Long targetUserGroupId; // If targetAudience is 'USER_GROUP'

	private Map<String, Object> targetCriteria; // JSONB criteria for custom targeting

	@Builder.Default
	private String priority = "NORMAL"; // LOW, NORMAL, HIGH, URGENT
}

