package com.tuttime.restaurant.controller;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.restaurant.dto.RestaurantDto;
import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.mapper.RestaurantMapper;
import com.tuttime.restaurant.service.MediaService;
import com.tuttime.restaurant.service.RestaurantService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RestaurantController.class)
@ActiveProfiles("test")
class RestaurantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestaurantService restaurantService;

    @MockBean
    private RestaurantMapper restaurantMapper;

    @MockBean
    private MediaService mediaService;

    private RestaurantDto sampleDto;
    private Restaurant sampleEntity;
    private UUID id;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GeometryFactory geometryFactory = new GeometryFactory();

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        Point location = geometryFactory.createPoint(new org.locationtech.jts.geom.Coordinate(40.7128, -74.0060));
        sampleDto = RestaurantDto.builder()
                .restaurantId(id)
                .ownerUserId(UUID.randomUUID())
                .name("Test Restaurant")
                .address("123 Test St")
                .location(location)
                .phone("+1234567890")
                .rating(BigDecimal.valueOf(4.5))
                .build();
        sampleEntity = Restaurant.builder()
                .restaurantId(id)
                .ownerUserId(sampleDto.getOwnerUserId())
                .name(sampleDto.getName())
                .address(sampleDto.getAddress())
                .location(sampleDto.getLocation())
                .phone(sampleDto.getPhone())
                .rating(sampleDto.getRating())
                .build();
    }

    @Test
    void getAllRestaurants_ShouldReturnList() throws Exception {
        // Given
        when(restaurantService.getAllRestaurants()).thenReturn(List.of(sampleEntity));
        when(restaurantMapper.toDto(sampleEntity)).thenReturn(sampleDto);

        // When & Then
        mockMvc.perform(get("/api/v1/restaurants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Restaurant"));

        verify(restaurantService).getAllRestaurants();
    }

    @Test
    void getRestaurantById_ShouldReturnRestaurant() throws Exception {
        // Given
        when(restaurantService.getRestaurantById(id)).thenReturn(sampleEntity);
        when(restaurantMapper.toDto(sampleEntity)).thenReturn(sampleDto);

        // When & Then
        mockMvc.perform(get("/api/v1/restaurants/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Restaurant"));

        verify(restaurantService).getRestaurantById(id);
    }

    @Test
    void createRestaurant_ShouldReturnCreated() throws Exception {
        // Given
        when(restaurantMapper.toEntity(sampleDto)).thenReturn(sampleEntity);
        when(restaurantService.createRestaurant(sampleEntity)).thenReturn(sampleEntity);
        when(restaurantMapper.toDto(sampleEntity)).thenReturn(sampleDto);

        // When & Then
        mockMvc.perform(post("/api/v1/restaurants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Restaurant"));

        verify(restaurantMapper).toEntity(sampleDto);
        verify(restaurantService).createRestaurant(sampleEntity);
    }

    @Test
    void updateRestaurant_ShouldReturnUpdated() throws Exception {
        // Given
        when(restaurantMapper.toEntity(sampleDto)).thenReturn(sampleEntity);
        when(restaurantService.updateRestaurant(id, sampleEntity)).thenReturn(sampleEntity);
        when(restaurantMapper.toDto(sampleEntity)).thenReturn(sampleDto);

        // When & Then
        mockMvc.perform(put("/api/v1/restaurants/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Restaurant"));

        verify(restaurantService).updateRestaurant(id, sampleEntity);
    }

    @Test
    void deleteRestaurant_ShouldReturnNoContent() throws Exception {
        // Given
        doNothing().when(restaurantService).deleteRestaurant(id);

        // When & Then
        mockMvc.perform(delete("/api/v1/restaurants/{id}", id))
                .andExpect(status().isNoContent());

        verify(restaurantService).deleteRestaurant(id);
    }

    // Add more tests for media endpoint, error cases, etc.
}
