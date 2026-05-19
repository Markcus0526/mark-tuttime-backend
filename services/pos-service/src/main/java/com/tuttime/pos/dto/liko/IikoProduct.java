package com.tuttime.pos.dto.liko;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IikoProduct {
    private String id;    // The POS ID (GUID)
    private String name;  // External dish name
    private double price; // Current price in iiko
}
