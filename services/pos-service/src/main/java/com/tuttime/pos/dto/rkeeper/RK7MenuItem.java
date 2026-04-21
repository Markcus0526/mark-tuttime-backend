package com.tuttime.pos.dto.rkeeper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RK7MenuItem {
	@JacksonXmlProperty(isAttribute = true, localName = "Ident")
	private String ident; // The primary numeric ID in RK7

	@JacksonXmlProperty(isAttribute = true, localName = "GUIDString")
	private String guid;  // The global unique ID string

	@JacksonXmlProperty(isAttribute = true, localName = "Name")
	private String name;

	@JacksonXmlProperty(isAttribute = true, localName = "Price")
	private Double price;
}
