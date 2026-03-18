package com.tuttime.analytics.repository;

import com.tuttime.analytics.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
  List<Event> findByTypeAndRestaurantId(String type, Long restaurantId);

  @Query("SELECT e FROM Event e WHERE e.restaurantId = ?1 AND e.timestamp > ?2")
  List<Event> findRecentByRestaurant(Long restaurantId, LocalDateTime since);
}
