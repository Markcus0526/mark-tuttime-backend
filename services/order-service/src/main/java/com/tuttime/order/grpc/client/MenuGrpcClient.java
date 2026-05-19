package com.tuttime.order.grpc.client;

import com.tuttime.common.event.grpc.GrpcExecutor;
import com.tuttime.common.event.proto.*;
import com.tuttime.common.util.NumberUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.tuttime.common.constants.ServiceNames.MENU_SERVICE;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuGrpcClient {

    @Value("${grpc.client-deadline-seconds}")
    private int grpcDeadline;

    @GrpcClient("menu-service") // Name matches config in application.yml
    private MenuServiceGrpc.MenuServiceBlockingStub menuServiceBlockingStub;

    private final GrpcExecutor grpcExecutor;

    public BigDecimal getPriceForOrder(UUID menuItemId, List<UUID> modifierIds) {
        GetPriceForOrderRequest request = GetPriceForOrderRequest.newBuilder()
                .setMenuItemId(menuItemId.toString())
                .addAllModifierIds(modifierIds.stream().map(UUID::toString).toList())
                .build();

        String priceString = grpcExecutor.execute(MENU_SERVICE, "getPriceForOrder", () ->
                menuServiceBlockingStub
                        .withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
                        .getPriceForOrder(request).getVerifiedTotalPrice());

        return NumberUtil.stringToBigDecimal(priceString);
    }

    public List<MenuItemDetailPto> getMenuDetails(List<String> menuItemIds) {
        // 1. Build the request with repeated IDs
        GetMenuDetailsRequest request = GetMenuDetailsRequest.newBuilder()
                .addAllMenuItemIds(menuItemIds)
                .build();

        return grpcExecutor.execute(MENU_SERVICE, "getMenuDetails", () ->
                menuServiceBlockingStub
                        .withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
                        .getMenuDetails(request).getItemsList());
    }

    public List<ActiveDiscountDto> getActiveOrderDiscounts(UUID branchId) {
        GetActiveOrderDiscountsRequest request = GetActiveOrderDiscountsRequest.newBuilder()
                .setBranchId(branchId.toString())
                .build();
        return grpcExecutor.execute(MENU_SERVICE, "getActiveOrderDiscounts", () ->
                menuServiceBlockingStub
                        .withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
                        .getActiveOrderDiscounts(request).getDiscountsList());
    }
}
