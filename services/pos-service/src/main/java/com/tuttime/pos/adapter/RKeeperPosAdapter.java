package com.tuttime.pos.adapter;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.tuttime.common.event.proto.GetOrderDetailResponse;
import com.tuttime.common.exception.PosSyncException;
import com.tuttime.pos.dto.rkeeper.RK7MenuResponse;
import com.tuttime.pos.dto.rkeeper.RK7Response;
import com.tuttime.pos.entity.PosConfig;
import com.tuttime.pos.entity.enums.PosProvider;
import com.tuttime.pos.grpc.client.OrderGrpcClient;
import com.tuttime.pos.service.PosMappingService;
import com.tuttime.pos.service.PosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RKeeperPosAdapter implements PosAdapter {

    private final WebClient webClient; // Configured for R-Keeper XML Gateway
    private final PosService posService;
    private final PosMappingService posMappingService;
    private final OrderGrpcClient orderGrpcClient;

    @Override
    public void reserveTable(PosConfig config, UUID tableId, UUID bookingId) {
        // 1. Format the XML Payload
        String xmlPayload = String.format(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<RK7Query><RK7Command2 CMD=\"CreateOrder\" TableID=\"%s\" " +
                        "OrderName=\"TutTime %s\" OrderType=\"1\"/></RK7Query>",
                tableId, bookingId
        );

        // 2. Use the helper method (This fixes the saveLog parameter count error)
        executeXmlSync(
                config,
                xmlPayload,
                "RESERVE_TABLE",
                bookingId,   // entityId
                "BOOKING"    // entityType
        );
    }

    @Override
    public PosProvider getProvider() {
        return PosProvider.RKEEPER;
    }

    @Override
    public void occupyTable(PosConfig config, UUID tableId, UUID bookingId) {
        // Tells R-Keeper to open a bill on the table, turning it 'Red' (Occupied) in the layout.
        String xmlPayload = String.format(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<RK7Query><RK7Command2 CMD=\"CreateOrder\" TableID=\"%s\" " +
                        "OrderType=\"1\"/></RK7Query>",
                tableId
        );
        executeXmlSync(config, xmlPayload, "OCCUPY_TABLE", bookingId, "BOOKING");
    }

    @Override
    public void releaseTable(PosConfig config, UUID tableId, UUID bookingId) {
        // Releasing requires finding the active order on the table and canceling it.
        // This is often done by a 'VoidOrder' or similar command.
        String xmlPayload = String.format(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<RK7Query><RK7Command2 CMD=\"VoidOrder\" TableID=\"%s\"/></RK7Query>",
                tableId
        );
        executeXmlSync(config, xmlPayload, "RELEASE_TABLE", bookingId, "BOOKING");
    }

    @Override
    public void sendToKitchen(PosConfig config, UUID orderId) {

    }

    @Override
    public void sendOrder(PosConfig config, UUID orderId) {
        GetOrderDetailResponse order = orderGrpcClient.getOrderDetail(orderId);

        // 1. Build the XML Items (Dishes) with validation
        StringBuilder itemsXml = new StringBuilder();

        for (var item : order.getItemsList()) {
            String posId = item.getPosId();

            // Fallback: If pos_id is missing, check the mapping table
            if (!StringUtils.hasText(posId)) {
                posId = posMappingService.getExternalId(
                        UUID.fromString(order.getBranchId()),
                        item.getMenuItemId()
                );
            }

            // CRITICAL VALIDATION: Fail fast if the ID is still missing
            if (!StringUtils.hasText(posId)) {
                throw new PosSyncException("R-Keeper Sync Error: Missing Ident for " + item.getMenuItemName());
            }

            // R-Keeper 7 standard dish tag
            itemsXml.append(String.format(
                    "<Dish id=\"%s\" count=\"%d\" />",
                    posId,
                    item.getQuantity()
            ));
        }

        // 2. Build the full XML payload
        // R-Keeper requires the Table id to open/save the session
        String xmlPayload = String.format(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<RK7Query><RK7Command2 CMD=\"SaveOrder\">" +
                        "  <Order>" +
                        "    <Table id=\"%s\"/>" +
                        "    <Session><OrderLines>%s</OrderLines></Session>" +
                        "  </Order>" +
                        "</RK7Command2></RK7Query>",
                order.getTableId(),
                itemsXml
        );

        // 3. Execute with 7-argument helper for logging
        executeXmlSync(
                config,
                xmlPayload,
                "SEND_ORDER",
                UUID.fromString(order.getOrderId()),
                "ORDER"
        );
    }

    @Override
    public void cancelOrder(PosConfig config, UUID orderId, String reason) {
        String xmlPayload = String.format(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<RK7Query><RK7Command2 CMD=\"VoidOrder\" OrderID=\"%s\">" +
                        "  <Comment>TutTime: %s</Comment>" +
                        "</RK7Command2></RK7Query>",
                orderId.toString(),
                reason
        );

        executeXmlSync(config, xmlPayload, "CANCEL_ORDER", orderId, "ORDER");
    }

    @Override
    public void refundOrder(PosConfig config, UUID orderId, BigDecimal amount, String reason) {
        String xmlPayload = String.format(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<RK7Query><RK7Command2 CMD=\"VoidPayment\" OrderID=\"%s\">" +
                        "  <Amount>%d</Amount>" +
                        "  <Reason id=\"%s\"/>" +
                        "  <Comment>%s</Comment>" +
                        "</RK7Command2></RK7Query>",
                orderId.toString(),
                amount.multiply(new BigDecimal(100)).intValue(), // Cents
//				config.getMetadata().get("rkeeperRefundReasonId"),TODO
                reason
        );

        executeXmlSync(config, xmlPayload, "REFUND_ORDER", orderId, "ORDER");
    }

    @Override
    public void syncMenu(PosConfig config) {
        // 1. Build the XML Command to pull Menu Items
        // OnlyActive="1" ensures you don't sync deleted dishes
        String xmlPayload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<RK7Query><RK7CMD CMD=\"GetRefData\" RefName=\"MenuItems\" OnlyActive=\"1\" " +
                "PropMask=\"Items.Ident,Items.Name,Items.Price\"/></RK7Query>";

        try {
            // 2. Execute XML Request via WebClient
            String rawXml = webClient.post()
                    .uri(config.getApiEndpoint())
                    .contentType(MediaType.APPLICATION_XML)
                    .headers(h -> h.setBasicAuth(config.getApiKey())) // R-Keeper typically uses Basic Auth
                    .bodyValue(xmlPayload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // 3. Parse XML and update mappings
            XmlMapper xmlMapper = new XmlMapper();
            RK7MenuResponse response = xmlMapper.readValue(rawXml, RK7MenuResponse.class);

            if (response != null && response.getItems() != null) {
                // Logic to update your pos_menu_mappings table
                posMappingService.updateRKeeperMappings(config.getBranchId(), response.getItems());
            }
        } catch (Exception e) {
            log.error("R-Keeper menu sync failed for branch {}: {}", config.getBranchId(), e.getMessage());
            throw new PosSyncException("R-Keeper Menu Sync Error", e);
        }
    }


    private void executeXmlSync(PosConfig config, String xmlPayload, String action, UUID entityId, String entityType) {
        try {
            String rawResponse = webClient.post()
                    .uri(config.getApiEndpoint())
                    .contentType(MediaType.APPLICATION_XML)
                    .headers(h -> h.setBasicAuth(config.getApiKey()))
                    .bodyValue(xmlPayload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // Parse XML response
            XmlMapper xmlMapper = new XmlMapper();
            RK7Response response = xmlMapper.readValue(rawResponse, RK7Response.class);

            if ("Ok".equalsIgnoreCase(response.getStatus())) {
                // FIX: Added entityId and entityType
                posService.saveLog(config.getBranchId(), entityId, entityType, action, xmlPayload, rawResponse, "SUCCESS");
            }
            // Handle "Table already occupied/reserved" as a success (Idempotency)
            else if (response.getErrorId() != null && response.getErrorId() == 1025) {
                log.warn("R-Keeper: Table already in state {} for branch {}", action, config.getBranchId());
                // FIX: Added entityId and entityType
                posService.saveLog(config.getBranchId(), entityId, entityType, action, xmlPayload, rawResponse, "SUCCESS_ALREADY_SET");
            } else {
                // FIX: Added entityId and entityType
                posService.saveLog(config.getBranchId(), entityId, entityType, action, xmlPayload, rawResponse, "FAILED");
                throw new PosSyncException("R-Keeper Error: " + response.getErrorText());
            }
        } catch (Exception e) {
            // FIX: Catch block must also pass entityId and entityType to saveLog
            posService.saveLog(config.getBranchId(), entityId, entityType, action, xmlPayload, e.getMessage(), "FAILED");
            throw new PosSyncException("R-Keeper communication error", e);
        }
    }
}
