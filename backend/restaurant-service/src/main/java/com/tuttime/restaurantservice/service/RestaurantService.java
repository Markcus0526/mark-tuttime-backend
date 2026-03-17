package com.tuttime.restaurantservice.service;

import com.tuttime.restaurantservice.dto.RestaurantRequest;
import com.tuttime.restaurantservice.dto.TableAvailabilityRequest;
import com.tuttime.restaurantservice.dto.TableAvailabilityResponse;
import com.tuttime.restaurantservice.entity.Restaurant;
import com.tuttime.restaurantservice.repository.RestaurantRepository;
import com.tuttime.restaurantservice.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestaurantService {

  private final RestaurantRepository restaurantRepository;
  private final TableRepository tableRepository;

  public Restaurant createRestaurant(RestaurantRequest request) {
    Restaurant restaurant = Restaurant.builder()
        .name(request.getName())
        .description(request.getDescription())
        .address(request.getAddress())
        .build();
    return restaurantRepository.save(restaurant);
  }

  public List<TableAvailabilityResponse> getAvailableTables(TableAvailabilityRequest request) {
    List<Table> available = tableRepository.findAvailableByBranchId(request.getBranchId());
    TableAvailabilityResponse response = new TableAvailabilityResponse();
    response.setAvailableTableIds(available.stream().map(Table::getId).toList());
    return List.of(response);
  }

}
