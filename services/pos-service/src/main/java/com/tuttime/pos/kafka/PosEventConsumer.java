package com.tuttime.pos.kafka;

import com.tuttime.common.kafka.BookingConfirmedEvent;
import com.tuttime.common.kafka.MenuUpdatedEvent;
import com.tuttime.common.kafka.OrderCreatedEvent;
import com.tuttime.pos.dto.BookingSyncRequest;
import com.tuttime.pos.dto.OrderSyncRequest;
import com.tuttime.pos.service.PosSyncService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import static com.tuttime.common.kafka.EventProducer.*;

@Service
@RequiredArgsConstructor
public class PosEventConsumer {

	private final PosSyncService posSyncService;

	@KafkaListener(
			topics = TOPIC_BOOKING_CONFIRMED,
			groupId = "pos-service-group",
			containerFactory = "bookingConfirmedKafkaListenerContainerFactory")
	public void consumeBookingConfirmed(ConsumerRecord<String, BookingConfirmedEvent> record) {
		BookingConfirmedEvent event = record.value();
		BookingSyncRequest request = new BookingSyncRequest(event.getBookingId(), event.getPosSystem());
		posSyncService.syncBooking(request);
	}

    @KafkaListener(
			topics = TOPIC_ORDER_CREATED,
		    groupId = "pos-service-group",
		    containerFactory = "orderCreatedKafkaListenerContainerFactory")
    public void consumeOrderCreated(ConsumerRecord<String, OrderCreatedEvent> record) {
        OrderCreatedEvent event = record.value();
        OrderSyncRequest request = new OrderSyncRequest(event.getOrderId(), event.getPosSystem());
        posSyncService.syncOrder(request);
    }

	@KafkaListener(
			topics = TOPIC_MENU_UPDATED,
			groupId = "pos-service-group",
			containerFactory = "menuUpdatedKafkaListenerContainerFactory")
	public void consumeMenuUpdated(ConsumerRecord<String, MenuUpdatedEvent> record) {
		MenuUpdatedEvent event = record.value();
		OrderSyncRequest request = new OrderSyncRequest(event.getRestaurantId(), event.getPosSystem());
		posSyncService.syncOrder(request);
	}
}