package com.tuttime.booking.feign;

import com.tuttime.booking.dto.TableAvailabilityDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDateTime;
import java.util.List;

@FeignClient(name = "restaurant-service")
public interface RestaurantClient {
  @GetMapping("/api/v1/restaurants/branches/{branchId}/tables/avail")
  List<TableAvailabilityDto> getAvailableTables(@PathVariable Long branchId,
      @RequestParam LocalDateTime slotStart);
}
