package com.tuttime.pos.dto.rkeeper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "RK7QueryResult")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RK7Response {
	@JacksonXmlProperty(isAttribute = true, localName = "Status")
	private String status; // e.g., "Ok" or "Error"

	@JacksonXmlProperty(isAttribute = true, localName = "ErrorID")
	private Integer errorId;

	@JacksonXmlProperty(isAttribute = true, localName = "ErrorText")
	private String errorText;
}

//ErrorID	Meaning	           Logic Strategy
//0	        Success	           Standard flow.
//1025	    Table Busy	       Ignore; the table is already in the state you wanted.
//1024	    Table Not Found	   Critical; check if the table_id matches the POS mapping.
//2001	    Invalid Auth	   Critical; the api_key in pos_configs is expired/wrong.