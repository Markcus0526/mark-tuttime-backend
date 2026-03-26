package com.tuttime.restaurant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.exception.ResourceNotFoundException;
import com.tuttime.restaurant.repository.BranchRepository;
import com.tuttime.restaurant.repository.RestaurantRepository;
import com.tuttime.restaurant.service.BranchService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BranchServiceTest {

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @InjectMocks
    private BranchService service;

    private Branch sampleBranch;
    private Restaurant sampleRestaurant;
    private UUID branchId;
    private UUID restaurantId;

    private final GeometryFactory geometryFactory = new GeometryFactory();

    @BeforeEach
    void setUp() {
        restaurantId = UUID.randomUUID();
        branchId = UUID.randomUUID();
        Point location = geometryFactory.createPoint(new org.locationtech.jts.geom.Coordinate(40.7128, -74.0060));
        sampleRestaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("Test Restaurant")
                .build();
        sampleBranch = Branch.builder()
                .branchId(branchId)
                .restaurant(sampleRestaurant)
                .name("Test Branch")
                .address("456 Branch St")
                .phone("+1987654321")
                .timezone("America/New_York")
                .location(location)
                .build();
    }

    @Test
    void getAll_ShouldReturnAll() {
        // Given
        when(branchRepository.findAll()).thenReturn(List.of(sampleBranch));

        // When
        List<Branch> result = service.getAll();

        // Then
        assertEquals(1, result.size());
        verify(branchRepository).findAll();
    }

    @Test
    void getById_ExistingId_ShouldReturnBranch() {
        // Given
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(sampleBranch));

        // When
        Branch result = service.getById(branchId);

        // Then
        assertEquals(sampleBranch, result);
        verify(branchRepository).findById(branchId);
    }

    @Test
    void getById_NonExistingId_ShouldThrowException() {
        // Given
        UUID nonExisting = UUID.randomUUID();
        when(branchRepository.findById(nonExisting)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.getById(nonExisting));
        assertTrue(ex.getMessage().contains("Branch not found"));
    }

    @Test
    void create_ValidRestaurant_ShouldSaveBranch() {
        // Given
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(sampleRestaurant));
        when(branchRepository.save(sampleBranch)).thenReturn(sampleBranch);

        // When
        Branch result = service.create(sampleBranch);

        // Then
        assertEquals(sampleBranch, result);
        verify(restaurantRepository).findById(restaurantId);
        verify(branchRepository).save(sampleBranch);
    }

    @Test
    void create_InvalidRestaurant_ShouldThrowException() {
        // Given
        UUID invalidRestId = UUID.randomUUID();
        sampleBranch.getRestaurant().setRestaurantId(invalidRestId); // Set invalid
        when(restaurantRepository.findById(invalidRestId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> service.create(sampleBranch));
        verify(branchRepository, never()).save(any());
    }

    @Test
    void update_ValidData_ShouldUpdateAndSave() {
        // Given
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(sampleBranch));
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(sampleRestaurant));
        when(branchRepository.save(any(Branch.class))).thenReturn(sampleBranch);

        sampleBranch.setName("Updated Branch"); // Update detail

        // When
        Branch result = service.update(branchId, sampleBranch);

        // Then
        assertEquals("Updated Branch", result.getName());
        verify(branchRepository).save(argThat(b -> b.getName().equals("Updated Branch")));
    }

    @Test
    void update_NonExistingBranch_ShouldThrow() {
        // Given
        when(branchRepository.findById(branchId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> service.update(branchId, sampleBranch));
    }

    @Test
    void delete_ExistingId_ShouldDelete() {
        // Given
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(sampleBranch));

        // When
        service.delete(branchId);

        // Then
        verify(branchRepository).delete(sampleBranch);
    }

    @Test
    void delete_NonExistingId_ShouldThrow() {
        // Given
        UUID nonExisting = UUID.randomUUID();
        when(branchRepository.findById(nonExisting)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> service.delete(nonExisting));
        verify(branchRepository, never()).delete(any());
    }
}
