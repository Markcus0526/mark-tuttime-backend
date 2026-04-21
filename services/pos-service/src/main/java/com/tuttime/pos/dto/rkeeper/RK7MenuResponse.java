package com.tuttime.pos.dto.rkeeper;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.util.List;

@Data
@JacksonXmlRootElement(localName = "RK7QueryResult")
public class RK7MenuResponse {
	@JacksonXmlProperty(localName = "RK7Reference")
	private RK7Reference reference;

	public List<RK7MenuItem> getItems() {
		return reference != null ? reference.getItems() : null;
	}
}
