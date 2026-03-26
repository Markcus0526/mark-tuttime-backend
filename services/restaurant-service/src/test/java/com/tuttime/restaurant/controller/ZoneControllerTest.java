package com.tuttime.restaurant.controller;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.restaurant.dto.ZoneDto;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.Zone;
import com.tuttime.restaurant.mapper.ZoneMapper;
import com.tuttime.restaurant.service.BranchService;
import com.tuttime.restaurant.service.ZoneService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(ZoneController.class)
@ActiveProfiles("test")
class ZoneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ZoneService zoneService;

    @MockBean
    private BranchService branchService;

    @MockBean
    private ZoneMapper zoneMapper;

    private ZoneDto sampleDto;
    private Zone sampleZone;
    private Branch sampleBranch;
    private UUID zoneId;
    private UUID branchId;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        branchId = UUID.randomUUID();
        zoneId = UUID.randomUUID();
        sampleBranch = Branch.builder().branchId(branchId).build();
        sampleDto = ZoneDto.builder()
                .zoneId(zoneId)
                .branchId(branchId)
                .name("Test Zone")
                .description("Test Desc")
                .capacity(50)
                .build();
        sampleZone = Zone.builder()
                .zoneId(zoneId)
                .branch(sampleBranch)
                .name("Test Zone")
                .description("Test Desc")
                .capacity(50)
                .build();
    }

    @Test
    void getAll() throws Exception {
        when(zoneService.getAll()).thenReturn(List.of(sampleZone));
        when(zoneMapper.toDto(sampleZone)).thenReturn(sampleDto);
        mockMvc.perform(get("/api/v1/zones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getById() throws Exception {
        when(zoneService.getById(zoneId)).thenReturn(sampleZone);
        when(zoneMapper.toDto(sampleZone)).thenReturn(sampleDto);
        mockMvc.perform(get("/api/v1/zones/{id}", zoneId))
                .andExpect(status().isOk());
    }

    @Test
    void create() throws Exception {
        when(branchService.getById(branchId)).thenReturn(sampleBranch);
        when(zoneMapper.toEntity(sampleDto, sampleBranch)).thenReturn(sampleZone);
        when(zoneService.create(sampleZone)).thenReturn(sampleZone);
        when(zoneMapper.toDto(sampleZone)).thenReturn(sampleDto);

        mockMvc.perform(post("/api/v1/zones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleDto)))
                .andExpect(status().isOk());
    }

    @Test
    void update() throws Exception {
        when(branchService.getById(branchId)).thenReturn(sampleBranch);
        when(zoneMapper.toEntity(sampleDto, sampleBranch)).thenReturn(sampleZone);
        when(zoneService.update(zoneId, sampleZone)).thenReturn(sampleZone);
        when(zoneMapper.toDto(sampleZone)).thenReturn(sampleDto);

        mockMvc.perform(put("/api/v1/zones/{id}", zoneId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleDto)))
                .andExpect(status().isOk());
    }

    @Test
    void delete() throws Exception {
        doNothing().when(zoneService).delete(zoneId);
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/zones/{id}", zoneId))
                .andExpect(status().isNoContent());
    }
}
