package com.tuttime.pos.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PosAdapterFactory {

	private final Map<String, PosAdapter> adapters;

	/**
	 * Get POS adapter by posSystem identifier.
	 * @param posSystemName e.g. "iiko", "r_keeper"
	 * @return corresponding PosAdapter
	 */
	public PosAdapter getAdapter(String posSystemName) {
		return adapters.values().stream()
				.filter(adapter -> adapter.getPosSystemName().equalsIgnoreCase(posSystemName))
                .findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unsupported POS system: " + posSystemName));
	}
}
