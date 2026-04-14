package com.tuttime.restaurant.kafka;

import com.tuttime.common.event.EventProducer;
import com.tuttime.common.event.RestaurantEvent;
import com.tuttime.restaurant.entity.Staff;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RestaurantEventProducer {

	private final EventProducer eventProducer;

	public void sendStaffUpdated(Staff staff) {
		UUID restaurantId = (staff.getRestaurant() != null) ? staff.getRestaurant().getId() : null;
		UUID branchId = (staff.getBranch() != null) ? staff.getBranch().getId() : null;

		RestaurantEvent.StaffUpdatedEvent event = new RestaurantEvent.StaffUpdatedEvent(
				staff.getId(),
				staff.getUserId(),
				restaurantId,
				branchId,
				staff.getRoleName(),
				staff.getActive(),
				staff.getUpdatedAt());
		eventProducer.sendRestaurantStaffUpdatedEvent(event);
		log.info("Broadcasted TOPIC_RESTAURANT_STAFF_UPDATED event: {}", staff.getId());
	}
}