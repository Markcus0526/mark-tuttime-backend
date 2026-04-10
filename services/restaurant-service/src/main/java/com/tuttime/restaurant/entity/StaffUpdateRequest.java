package com.tuttime.restaurant.entity;

// Request DTO for updates
public record StaffUpdateRequest(
		String roleName,
		Boolean isActive
) {}
