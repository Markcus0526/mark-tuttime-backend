package com.tuttime.pos.service;

import com.tuttime.pos.dto.liko.IikoProduct;
import com.tuttime.pos.dto.rkeeper.RK7MenuItem;

import java.util.List;
import java.util.UUID;

public interface PosMappingService {

    void updateIikoMappings(UUID branchId, List<IikoProduct> products);

    void updateRKeeperMappings(UUID branchId, List<RK7MenuItem> rkItems);

    String getExternalId(UUID branchId, String menuItemId);
}
