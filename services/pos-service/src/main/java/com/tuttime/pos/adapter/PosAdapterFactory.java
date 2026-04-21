package com.tuttime.pos.adapter;

import com.tuttime.pos.entity.enums.PosProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PosAdapterFactory {

	private final Map<PosProvider, PosAdapter> adapters;

	// Spring automatically injects all beans implementing PosAdapter into this list
	public PosAdapterFactory(List<PosAdapter> adapterList) {
		this.adapters = adapterList.stream()
				.collect(Collectors.toMap(PosAdapter::getProvider, Function.identity()));
	}

	public PosAdapter getAdapter(PosProvider provider) {
		return Optional.ofNullable(adapters.get(provider))
				.orElseThrow(() -> new UnsupportedOperationException("Provider not supported: " + provider));
	}
}