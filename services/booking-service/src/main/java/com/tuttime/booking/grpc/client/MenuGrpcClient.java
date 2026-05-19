package com.tuttime.booking.grpc.client;

import com.tuttime.common.event.grpc.GrpcExecutor;
import com.tuttime.common.event.proto.GetMenuDetailsRequest;
import com.tuttime.common.event.proto.GetPreOrderMenuRequest;
import com.tuttime.common.event.proto.MenuItemDetailPto;
import com.tuttime.common.event.proto.MenuServiceGrpc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    @GrpcClient("menu-service")
    private MenuServiceGrpc.MenuServiceBlockingStub menuServiceBlockingStub;

    private final GrpcExecutor grpcExecutor;

    public List<MenuItemDetailPto> getPreOrderMenu(UUID branchId) {
        GetPreOrderMenuRequest request = GetPreOrderMenuRequest.newBuilder()
                .setBranchId(branchId.toString())
                .build();
        return grpcExecutor.execute(MENU_SERVICE, "getPreOrderMenu", () ->
                menuServiceBlockingStub
                        .withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
                        .getPreOrderMenu(request).getItemsList());
    }

    public List<MenuItemDetailPto> getMenuDetails(List<String> menuItemIds) {
        GetMenuDetailsRequest request = GetMenuDetailsRequest.newBuilder()
                .addAllMenuItemIds(menuItemIds)
                .build();

        return grpcExecutor.execute(MENU_SERVICE, "getMenuDetails", () ->
                menuServiceBlockingStub
                        .withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
                        .getMenuDetails(request).getItemsList());
    }
}