package com.tuttime.restaurant.service;

import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.exception.ResourceNotFoundException;
import com.tuttime.restaurant.repository.BranchRepository;
import com.tuttime.restaurant.repository.RestaurantRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BranchService {

	private final BranchRepository branchRepository;
	private final RestaurantRepository restaurantRepository;

	public BranchService(BranchRepository branchRepository, RestaurantRepository restaurantRepository) {
		this.branchRepository = branchRepository;
		this.restaurantRepository = restaurantRepository;
	}

	public List<Branch> getAll() {
		return branchRepository.findAll();
	}

	public Branch getById(Long id) {
		return branchRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found with id " + id));
	}

	public Branch create(Branch branch) {
		// Validate restaurant exists
		Long restaurantId = branch.getRestaurant().getRestaurantId();
		Restaurant restaurant = restaurantRepository.findById(restaurantId)
				.orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id " + restaurantId));
		branch.setRestaurant(restaurant);
		return branchRepository.save(branch);
	}

	public Branch update(Long id, Branch details) {
		Branch existing = getById(id);
		Long restaurantId = details.getRestaurant().getRestaurantId();
		Restaurant restaurant = restaurantRepository.findById(restaurantId)
				.orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id " + restaurantId));

		existing.setRestaurant(restaurant);
		existing.setName(details.getName());
		existing.setAddress(details.getAddress());
		existing.setPhone(details.getPhone());
		existing.setTimezone(details.getTimezone());
		existing.setLocation(details.getLocation());

		return branchRepository.save(existing);
	}

	public void delete(Long id) {
		Branch existing = getById(id);
		branchRepository.delete(existing);
	}
}
