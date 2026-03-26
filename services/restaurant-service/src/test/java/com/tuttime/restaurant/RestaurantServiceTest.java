package com.tuttime.restaurant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.exception.ResourceNotFoundException;
import com.tuttime.restaurant.repository.RestaurantRepository;
import com.tuttime.restaurant.service.RestaurantService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock
    private RestaurantRepository repository;

    @InjectMocks
    private RestaurantService service;

    private Restaurant sampleRestaurant;
    private UUID restaurantId;

    private final GeometryFactory geometryFactory = new GeometryFactory();

    @BeforeEach
    void setUp() {
        restaurantId = UUID.randomUUID();
        Point location = geometryFactory.createPoint(new org.locationtech.jts.geom.Coordinate(40.7128, -74.0060));
        sampleRestaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .ownerUserId(UUID.randomUUID())
                .name("Test Restaurant")
                .address("123 Test St")
                .location(location)
                .phone("+1234567890")
                .rating(BigDecimal.valueOf(4.5))
                .build();
    }

    @Test
    void getAllRestaurants_ShouldReturnAll() {
        // Given
        Restaurant another = Restaurant.builder().restaurantId(UUID.randomUUID()).name("Another").build();
        when(repository.findAll()).thenReturn(List.of(sampleRestaurant, another));

        // When
        List<Restaurant> result = service.getAllRestaurants();

        // Then
        assertEquals(2, result.size());
        verify(repository).findAll();
    }

    @Test
    void getRestaurantById_ExistingId_ShouldReturnRestaurant() {
        // Given
        when(repository.findById(restaurantId)).thenReturn(Optional.of(sampleRestaurant));

        // When
        Restaurant result = service.getRestaurantById(restaurantId);

        // Then
        assertEquals(sampleRestaurant, result);
        verify(repository).findById(restaurantId);
    }

    @Test
    void getRestaurantById_NonExistingId_ShouldThrowException() {
        // Given
        UUID nonExistingId = UUID.randomUUID();
        when(repository.findById(nonExistingId)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> service.getRestaurantById(nonExistingId));
        assertTrue(exception.getMessage().contains("Restaurant not found"));
        verify(repository).findById(nonExistingId);
    }

    @Test
    void createRestaurant_ShouldSaveAndReturn() {
        // Given
        when(repository.save(any(Restaurant.class))).thenReturn(sampleRestaurant);

        // When
        Restaurant result = service.createRestaurant(sampleRestaurant);

        // Then
        assertEquals(sampleRestaurant, result);
        verify(repository).save(sampleRestaurant);
    }

    @Test
    void updateRestaurant_ExistingId_ShouldUpdateFieldsAndSave() {
        // Given
        when(repository.findById(restaurantId)).thenReturn(Optional.of(sampleRestaurant));
        when(repository.save(any(Restaurant.class))).thenReturn(sampleRestaurant);

        sampleRestaurant.setName("Updated Name"); // Simulate update details

        // When
        Restaurant result = service.updateRestaurant(restaurantId, sampleRestaurant);

        // Then
        assertEquals("Updated Name", result.getName());
        verify(repository).findById(restaurantId);
        verify(repository).save(any(Restaurant.class));
    }

    @Test
    void updateRestaurant_NonExistingId_ShouldThrowException() {
        // Given
        UUID nonExistingId = UUID.randomUUID();
        when(repository.findById(nonExistingId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> service.updateRestaurant(nonExistingId, sampleRestaurant));
        verify(repository).findById(nonExistingId);
        verify(repository, never()).save(any());
    }

    @Test
    void deleteRestaurant_ExistingId_ShouldDelete() {
        // Given
        when(repository.findById(restaurantId)).thenReturn(Optional.of(sampleRestaurant));

        // When
        service.deleteRestaurant(restaurantId);

        // Then
        verify(repository).findById(restaurantId);
        verify(repository).delete(sampleRestaurant);
    }

    @Test
    void deleteRestaurant_NonExistingId_ShouldThrowException() {
        // Given
        UUID nonExistingId = UUID.randomUUID();
        when(repository.findById(nonExistingId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> service.deleteRestaurant(nonExistingId));
        verify(repository).findById(nonExistingId);
        verify(repository, never()).delete(any());
    }
}
