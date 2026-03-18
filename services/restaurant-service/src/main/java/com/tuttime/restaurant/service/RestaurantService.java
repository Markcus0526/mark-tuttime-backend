package com.tuttime.restaurant.service;

import com.tuttime.restaurant.dto.RestaurantRequest;
import com.tuttime.restaurant.dto.TableAvailabilityRequest;
import com.tuttime.restaurant.dto.TableAvailabilityResponse;
import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.repository.RestaurantRepository;
import com.tuttime.restaurant.entity.Table;
import com.tuttime.restaurant.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestaurantService {

  private final RestaurantRepository restaurantRepository;
  private final TableRepository tableRepository;

  public Restaurant createRestaurant(RestaurantRequest request) {
    Restaurant restaurant = new Restaurant();
    restaurant.setName(request.getName());
    restaurant.setDescription(request.getDescription());
    restaurant.setAddress(request.getAddress());
    return restaurantRepository.save(restaurant);
  }

  public List<TableAvailabilityResponse> getAvailableTables(TableAvailabilityRequest request) {
    List<Table> available = tableRepository.findAvailableByBranchId(request.getBranchId());
    TableAvailabilityResponse response = new TableAvailabilityResponse();
    response.setAvailableTableIds(available.stream().map(Table::getId).toList());
    return List.of(response);
  }

}
