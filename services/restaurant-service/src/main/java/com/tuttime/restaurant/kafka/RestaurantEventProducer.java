package com.tuttime.restaurant.kafka;

import com.tuttime.common.event.EventProducer;
import com.tuttime.common.event.RestaurantEvent;
import com.tuttime.restaurant.entity.RestaurantStaff;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RestaurantEventProducer {

	private final EventProducer eventProducer;

	public void sendStaffUpdated(RestaurantStaff staff) {
		// We reuse the "USER_BLOCKED" type logic at the Gateway
		// because a deleted user should never be allowed to authenticate again.
		RestaurantEvent.StaffUpdatedEvent event = new RestaurantEvent.StaffUpdatedEvent(
				staff.getStaffId(),
				staff.getUserId(),
				staff.getRestaurantId(),
				staff.getBranchId(),
				staff.getRoleName(),
				staff.getActive(),
				staff.getUpdatedAt());
		eventProducer.sendRestaurantStaffUpdatedEvent(event);
		log.info("Broadcasted TOPIC_RESTAURANT_STAFF_UPDATED event: {}", staff.getStaffId());
	}
}