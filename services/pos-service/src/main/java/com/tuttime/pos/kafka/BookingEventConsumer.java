package com.tuttime.pos.kafka;

import com.tuttime.common.event.BookingEvent;
import com.tuttime.pos.dto.BookingSyncRequest;
import com.tuttime.pos.dto.OrderSyncRequest;
import com.tuttime.pos.service.PosSyncService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.tuttime.common.event.EventProducer.TOPIC_BOOKING_CONFIRMED;

@Service
@RequiredArgsConstructor
public class BookingEventConsumer {

	private final PosSyncService posSyncService;

	@KafkaListener(
			topics = TOPIC_BOOKING_CONFIRMED,
			groupId = "pos-service-group",
			containerFactory = "bookingConfirmedKafkaListenerContainerFactory")
	public void consumeBookingConfirmed(ConsumerRecord<String, BookingEvent.BookingConfirmedEvent> record) {
		BookingEvent.BookingConfirmedEvent event = record.value();
		BookingSyncRequest request = new BookingSyncRequest(UUID.fromString(event.restaurantId()), event.posSystem());
		posSyncService.syncBooking(request);
	}
}