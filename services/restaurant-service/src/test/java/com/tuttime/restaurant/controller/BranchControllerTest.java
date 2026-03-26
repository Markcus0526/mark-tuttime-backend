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

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.restaurant.dto.BranchDto;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.mapper.BranchMapper;
import com.tuttime.restaurant.service.BranchService;
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

@WebMvcTest(BranchController.class)
@ActiveProfiles("test")
class BranchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BranchService branchService;

    @MockBean
    private RestaurantService restaurantService;

    @MockBean
    private BranchMapper branchMapper;

    @MockBean
    private MediaService mediaService;

    private BranchDto sampleDto;
    private Branch sampleBranch;
    private Restaurant sampleRestaurant;
    private UUID branchId;
    private UUID restaurantId;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GeometryFactory geometryFactory = new GeometryFactory();

    @BeforeEach
    void setUp() {
        restaurantId = UUID.randomUUID();
        branchId = UUID.randomUUID();
        Point location = geometryFactory.createPoint(new org.locationtech.jts.geom.Coordinate(40.7128, -74.0060));
        sampleRestaurant = Restaurant.builder().restaurantId(restaurantId).build();
        sampleDto = BranchDto.builder()
                .branchId(branchId)
                .restaurantId(restaurantId)
                .name("Test Branch")
                .address("456 Branch St")
                .phone("+1987654321")
                .timezone("America/New_York")
                .location(location)
                .build();
        sampleBranch = Branch.builder()
                .branchId(branchId)
                .restaurant(sampleRestaurant)
                .name(sampleDto.getName())
                .address(sampleDto.getAddress())
                .phone(sampleDto.getPhone())
                .timezone(sampleDto.getTimezone())
                .location(sampleDto.getLocation())
                .build();
    }

    @Test
    void getAll_ShouldReturnList() throws Exception {
        // Given
        when(branchService.getAll()).thenReturn(List.of(sampleBranch));
        when(branchMapper.toDto(sampleBranch)).thenReturn(sampleDto);

        // When & Then
        mockMvc.perform(get("/api/v1/branches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(branchService).getAll();
    }

    @Test
    void getById_ShouldReturnBranch() throws Exception {
        // Given
        when(branchService.getById(branchId)).thenReturn(sampleBranch);
        when(branchMapper.toDto(sampleBranch)).thenReturn(sampleDto);

        // When & Then
        mockMvc.perform(get("/api/v1/branches/{id}", branchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Branch"));
    }

    @Test
    void createBranch_ShouldReturnCreated() throws Exception {
        // Given
        when(restaurantService.getRestaurantById(restaurantId)).thenReturn(sampleRestaurant);
        when(branchMapper.toEntity(sampleDto, sampleRestaurant)).thenReturn(sampleBranch);
        when(branchService.create(sampleBranch)).thenReturn(sampleBranch);
        when(branchMapper.toDto(sampleBranch)).thenReturn(sampleDto);

        // When & Then
        mockMvc.perform(post("/api/v1/branches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Branch"));
    }

    @Test
    void updateBranch_ShouldReturnUpdated() throws Exception {
        // Given
        when(restaurantService.getRestaurantById(restaurantId)).thenReturn(sampleRestaurant);
        when(branchMapper.toEntity(sampleDto, sampleRestaurant)).thenReturn(sampleBranch);
        when(branchService.update(branchId, sampleBranch)).thenReturn(sampleBranch);
        when(branchMapper.toDto(sampleBranch)).thenReturn(sampleDto);

        // When & Then
        mockMvc.perform(put("/api/v1/branches/{id}", branchId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleDto)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteBranch_ShouldReturnNoContent() throws Exception {
        // Given
        doNothing().when(branchService).delete(branchId);

        // When & Then
        mockMvc.perform(delete("/api/v1/branches/{id}", branchId))
                .andExpect(status().isNoContent());
    }

    // Media endpoint test
    @Test
    void getMediaByBranch_ShouldReturnMediaList() throws Exception {
        // Given - mock mediaService in controller
        // Implementation depends on MediaService; stub for now

        mockMvc.perform(get("/api/v1/branches/{id}/media", branchId))
                .andExpect(status().isOk());
    }
}
