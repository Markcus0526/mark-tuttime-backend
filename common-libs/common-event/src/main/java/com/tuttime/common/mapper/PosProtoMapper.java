package com.tuttime.common.mapper;

import com.google.protobuf.util.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.tuttime.common.proto.OrderDetailsResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PosProtoMapper {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	public static Map<String, Object> toPosMap(OrderDetailsResponse response) throws Exception {
		// 1. Initial conversion from Proto to Map
		String json = JsonFormat.printer()
				.includingDefaultValueFields()
				.print(response);

		Map<String, Object> rawMap = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});

		// 2. Custom remapping for POS (iiko/R-Keeper)
		Map<String, Object> posMap = new HashMap<>(rawMap);

		// Example: Rename 'order_id' to 'id' for iiko
		if (posMap.containsKey("order_id")) {
			posMap.put("id", posMap.remove("order_id"));
		}

		// Example: Rename 'total_amount' to 'sum' for R-Keeper
		if (posMap.containsKey("total_amount")) {
			posMap.put("sum", posMap.remove("total_amount"));
		}

		return posMap;
	}

	/**
	 * Helper to navigate the 'items' list and rename internal keys
	 */
	@SuppressWarnings("unchecked")
	private static void renameNestedItems(Map<String, Object> posMap) {
		// gRPC 'repeated' fields become a List in the Map
		List<Map<String, Object>> items = (List<Map<String, Object>>) posMap.get("items");

		if (items != null) {
			for (Map<String, Object> item : items) {
				// Rename 'menu_item_name' -> 'name' (common POS requirement)
				if (item.containsKey("menu_item_name")) {
					item.put("name", item.remove("menu_item_name"));
				}
				// Rename 'quantity' -> 'amount' if needed by R-Keeper
				if (item.containsKey("quantity")) {
					item.put("amount", item.get("quantity"));
				}
			}
		}
	}
}