package com.tuttime.restaurant.repository.projection;

import java.util.UUID;

public interface BranchDetailProjection {
	UUID getBranchId();
	UUID getRestaurantId();
	String getBranchName();
	String getBranchAddress();
	String getBranchPhone();
	String getRestaurantName();
	Integer getMinPrice();
	Integer getMaxPrice();
}
