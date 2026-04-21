package com.tuttime.pos.dto.liko;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IikoErrorResponse {
	private String message;
	private String errorDescription;
	private String error; // internal code like "INVALID_BODY_JSON_FORMAT"
}

//HTTP Code   Message/Error Key	              Logic Strategy
//400	      INVALID_BODY_JSON_FORMAT	      Critical: Fix your Java DTO or JSON mapping.
//401	      Unauthorized	                  Critical: Your api_key has expired; needs a fresh login.
//403	      Access Denied	                  Critical: Check organization_id permissions.
//409	      Conflict (e.g., "Table Busy")	  Ignore/Warn: The table is already in the desired state.
//480	      Terminal Offline	              Retry: The physical restaurant terminal is disconnected.